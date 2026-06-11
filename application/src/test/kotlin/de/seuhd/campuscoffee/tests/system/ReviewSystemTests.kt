package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.dtos.ReviewDto
import de.seuhd.campuscoffee.domain.configuration.ApprovalConfiguration
import de.seuhd.campuscoffee.domain.model.objects.Pos
import de.seuhd.campuscoffee.domain.model.objects.User
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import de.seuhd.campuscoffee.tests.SystemTestUtils.reviewRequests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

/**
 * System tests for the operations related to reviews, including the approval workflow.
 * The default approval quorum is `campus-coffee.approval.min-count = 3`, so a review needs three
 * approvals from users other than the author to become approved.
 */
class ReviewSystemTests : AbstractSystemTest() {
    @Autowired
    private lateinit var approvalConfiguration: ApprovalConfiguration

    @Test
    fun `creating a review returns it unapproved`() {
        val pos = createPos()
        val author = createUser("author", "author@uni-heidelberg.de")

        val created =
            reviewRequests
                .create(listOf(reviewFor(pos, author, "Solid espresso and plenty of seating.")))
                .first()

        assertThat(created.approved).isFalse()
    }

    @Test
    fun `listing reviews and fetching one by id return the created review`() {
        val pos = createPos()
        val author = createUser("author", "author@uni-heidelberg.de")
        val created =
            reviewRequests
                .create(listOf(reviewFor(pos, author, "A reliable spot between lectures.")))
                .first()

        assertThat(reviewRequests.retrieveAll().map { it.id }).containsExactly(created.id)

        val byId = reviewRequests.retrieveById(created.id!!)
        assertThat(byId.review).isEqualTo("A reliable spot between lectures.")
    }

    @Test
    fun `updating a review changes its text`() {
        val pos = createPos()
        val author = createUser("author", "author@uni-heidelberg.de")
        val created =
            reviewRequests
                .create(listOf(reviewFor(pos, author, "Original review text, long enough.")))
                .first()

        val updated =
            reviewRequests
                .update(listOf(created.copy(review = "Updated review text, also long enough.")))
                .first()

        assertThat(updated.review).isEqualTo("Updated review text, also long enough.")
        assertThat(reviewRequests.retrieveById(created.id!!).review)
            .isEqualTo("Updated review text, also long enough.")
    }

    @Test
    fun `updating a review keeps its approval state`() {
        val pos = createPos()
        val author = createUser("author", "author@uni-heidelberg.de")
        // exactly the configured quorum of approvers, so the test does not depend on min-count = 3
        val approvers =
            (1..approvalConfiguration.minCount)
                .map { createUser("approver_$it", "approver$it@uni-heidelberg.de") }
        val created =
            reviewRequests
                .create(listOf(reviewFor(pos, author, "Review text before the update.")))
                .first()
        approvers.forEach { reviewRequests.approve(created.id!!, it.id!!) }
        assertThat(reviewRequests.retrieveById(created.id!!).approved).isTrue()

        // approvals are managed by the approval workflow; a text edit must not erase them
        val updated =
            reviewRequests
                .update(listOf(created.copy(review = "Review text after the update.")))
                .first()

        assertThat(updated.review).isEqualTo("Review text after the update.")
        assertThat(updated.approved).isTrue()
        assertThat(reviewRequests.retrieveById(created.id!!).approved).isTrue()
    }

    @Test
    fun `re-pointing a review at a different POS returns 400 Bad Request`() {
        val firstPos = createPos()
        val secondPos =
            posService.upsert(
                TestFixtures.getPosFixturesForInsertion().last().copy(name = "Second POS for the move test")
            )
        val author = createUser("author", "author@uni-heidelberg.de")
        val reviewOnFirstPos =
            reviewRequests
                .create(listOf(reviewFor(firstPos, author, "Review for the first POS.")))
                .first()
        val reviewOnSecondPos =
            reviewRequests
                .create(listOf(reviewFor(secondPos, author, "Review for the second POS.")))
                .first()

        // a review's POS and author are fixed at creation: re-pointing the second review at the first
        // POS would yield two reviews by the same author and carry approvals to the wrong POS
        val movedReview = reviewOnSecondPos.copy(posId = firstPos.id)
        val statusCode =
            reviewRequests
                .updateAndReturnStatusCodes(listOf(movedReview))
                .first()

        assertThat(statusCode).isEqualTo(HttpStatus.BAD_REQUEST.value())
        // both reviews still point at their original POS
        assertThat(reviewRequests.retrieveById(reviewOnFirstPos.id!!).posId).isEqualTo(firstPos.id)
        assertThat(reviewRequests.retrieveById(reviewOnSecondPos.id!!).posId).isEqualTo(secondPos.id)
    }

    @Test
    fun `updating a review that does not exist returns 404 Not Found`() {
        val pos = createPos()
        val author = createUser("author", "author@uni-heidelberg.de")
        val existing =
            reviewRequests
                .create(listOf(reviewFor(pos, author, "The author's only review, long enough.")))
                .first()

        // unknown id, but the same author/POS pair as the existing review: the missing id must win
        // (404), not the duplicate rule (409)
        val ghost = existing.copy(id = 9999L)
        val statusCode = reviewRequests.updateAndReturnStatusCodes(listOf(ghost)).first()

        assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `deleting a review twice returns 204 No Content then 404 Not Found`() {
        val pos = createPos()
        val author = createUser("author", "author@uni-heidelberg.de")
        val created =
            reviewRequests
                .create(listOf(reviewFor(pos, author, "This review will be deleted.")))
                .first()
        val id = requireNotNull(created.id)

        val statusCodes = reviewRequests.deleteAndReturnStatusCodes(listOf(id, id))

        // the first delete returns 204 No Content, the second 404 Not Found
        assertThat(statusCodes).containsExactly(HttpStatus.NO_CONTENT.value(), HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `approving a review below the quorum leaves it unapproved`() {
        val pos = createPos()
        val author = createUser("author", "author@uni-heidelberg.de")
        val firstApprover = createUser("approver_one", "approver.one@uni-heidelberg.de")
        val secondApprover = createUser("approver_two", "approver.two@uni-heidelberg.de")
        val review =
            reviewRequests
                .create(listOf(reviewFor(pos, author, "Review that stays below the quorum.")))
                .first()

        reviewRequests.approve(review.id!!, firstApprover.id!!)
        val afterTwoApprovals = reviewRequests.approve(review.id!!, secondApprover.id!!)

        assertThat(afterTwoApprovals.approved).isFalse()
    }

    @Test
    fun `approving a review up to the quorum marks it approved`() {
        val pos = createPos()
        val author = createUser("author", "author@uni-heidelberg.de")
        val firstApprover = createUser("approver_one", "approver.one@uni-heidelberg.de")
        val secondApprover = createUser("approver_two", "approver.two@uni-heidelberg.de")
        val thirdApprover = createUser("approver_three", "approver.three@uni-heidelberg.de")
        val review =
            reviewRequests
                .create(listOf(reviewFor(pos, author, "Review that reaches the quorum.")))
                .first()

        reviewRequests.approve(review.id!!, firstApprover.id!!)
        reviewRequests.approve(review.id!!, secondApprover.id!!)
        val afterThreeApprovals = reviewRequests.approve(review.id!!, thirdApprover.id!!)

        assertThat(afterThreeApprovals.approved).isTrue()
    }

    @Test
    fun `approving your own review returns 400 Bad Request`() {
        val pos = createPos()
        val author = createUser("author", "author@uni-heidelberg.de")
        val review =
            reviewRequests
                .create(listOf(reviewFor(pos, author, "Author tries to approve this review.")))
                .first()

        val statusCode = reviewRequests.approveAndReturnStatusCode(review.id!!, author.id!!)

        assertThat(statusCode).isEqualTo(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `creating a second review by the same author for a POS returns 409 Conflict`() {
        val pos = createPos()
        val author = createUser("author", "author@uni-heidelberg.de")
        reviewRequests.create(listOf(reviewFor(pos, author, "First review by this author.")))

        // a duplicate is a conflict (the same status the uq_reviews_pos_author constraint produces
        // when two concurrent creates race past the application-level check)
        val statusCode =
            reviewRequests
                .createAndReturnStatusCodes(listOf(reviewFor(pos, author, "Second review by the same author.")))
                .first()

        assertThat(statusCode).isEqualTo(HttpStatus.CONFLICT.value())
    }

    @Test
    fun `approving a missing review returns 404 Not Found`() {
        val approver = createUser("approver", "approver@uni-heidelberg.de")

        val statusCode = reviewRequests.approveAndReturnStatusCode(9999L, approver.id!!)

        assertThat(statusCode).isEqualTo(HttpStatus.NOT_FOUND.value())
    }

    @Test
    fun `filtering reviews by approval status returns only the matching reviews`() {
        val pos = createPos()
        val approvedAuthor = createUser("approved_author", "approved.author@uni-heidelberg.de")
        val pendingAuthor = createUser("pending_author", "pending.author@uni-heidelberg.de")
        val firstApprover = createUser("approver_one", "approver.one@uni-heidelberg.de")
        val secondApprover = createUser("approver_two", "approver.two@uni-heidelberg.de")
        val thirdApprover = createUser("approver_three", "approver.three@uni-heidelberg.de")

        val approvedReview =
            reviewRequests
                .create(listOf(reviewFor(pos, approvedAuthor, "This review reaches the quorum.")))
                .first()
        val pendingReview =
            reviewRequests
                .create(listOf(reviewFor(pos, pendingAuthor, "This review stays below the quorum.")))
                .first()

        reviewRequests.approve(approvedReview.id!!, firstApprover.id!!)
        reviewRequests.approve(approvedReview.id!!, secondApprover.id!!)
        reviewRequests.approve(approvedReview.id!!, thirdApprover.id!!)

        val posId = requireNotNull(pos.id)
        val approved = reviewRequests.retrieveByFilter(mapOf("pos_id" to posId, "approved" to true))
        assertThat(approved.map { it.id }).containsExactly(approvedReview.id)

        val pending = reviewRequests.retrieveByFilter(mapOf("pos_id" to posId, "approved" to false))
        assertThat(pending.map { it.id }).containsExactly(pendingReview.id)
    }

    // helpers ---------------------------------------------------------------------

    private fun createPos(): Pos = posService.upsert(TestFixtures.getPosFixturesForInsertion().first())

    private fun createUser(
        loginName: String,
        emailAddress: String
    ): User =
        userService.upsert(
            User(loginName = loginName, emailAddress = emailAddress, firstName = "First", lastName = "Last")
        )

    private fun reviewFor(
        pos: Pos,
        author: User,
        text: String
    ): ReviewDto = ReviewDto(posId = pos.id, authorId = author.id, review = text)
}
