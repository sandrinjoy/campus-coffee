package de.seuhd.campuscoffee.data.integration;

import de.seuhd.campuscoffee.data.mapper.PosEntityMapper;
import de.seuhd.campuscoffee.data.mapper.UserEntityMapper;
import de.seuhd.campuscoffee.data.persistence.entities.PosEntity;
import de.seuhd.campuscoffee.data.persistence.entities.ReviewEntity;
import de.seuhd.campuscoffee.data.persistence.entities.UserEntity;
import de.seuhd.campuscoffee.domain.tests.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Optimistic locking on the review version column: two snapshots of the same review are loaded, the first
 * save succeeds, and the second save fails because its version is now stale.
 */
class OptimisticLockingIntegrationTest extends AbstractDataIntegrationTest {

    @Autowired
    private PosEntityMapper posEntityMapper;

    @Autowired
    private UserEntityMapper userEntityMapper;

    @Test
    void staleReviewUpdateIsRejected() {
        UserEntity author = userRepository.saveAndFlush(
                userEntityMapper.toEntity(TestFixtures.getUserFixturesForInsertion().getFirst()));
        PosEntity pos = posRepository.saveAndFlush(
                posEntityMapper.toEntity(TestFixtures.getPosFixturesForInsertion().getFirst()));

        ReviewEntity review = new ReviewEntity();
        review.setPos(pos);
        review.setAuthor(author);
        review.setReview("Great place!");
        review.setApprovalCount(0);
        review.setApproved(false);
        Long id = reviewRepository.saveAndFlush(review).getId();

        // each findById runs in its own transaction and returns a detached entity, so these are two
        // independent snapshots both at the initial version
        ReviewEntity first = reviewRepository.findById(id).orElseThrow();
        ReviewEntity stale = reviewRepository.findById(id).orElseThrow();

        // the first write succeeds and increments the version
        first.setApprovalCount(1);
        reviewRepository.saveAndFlush(first);

        // the second write carries the now-outdated version and must fail
        stale.setApprovalCount(2);
        assertThatThrownBy(() -> reviewRepository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
