package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.dtos.PosDto
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import de.seuhd.campuscoffee.tests.SystemTestUtils.assertEqualsIgnoringIdAndTimestamps
import de.seuhd.campuscoffee.tests.SystemTestUtils.assertEqualsIgnoringTimestamps
import de.seuhd.campuscoffee.tests.SystemTestUtils.client
import de.seuhd.campuscoffee.tests.SystemTestUtils.posRequests
import de.seuhd.campuscoffee.tests.SystemTestUtils.withAuth
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.returnResult

/**
 * System tests for the operations related to POS (Point of Sale).
 */
class PosSystemTests : AbstractSystemTest() {
    @Test
    fun `creating a POS returns it with the same field values`() {
        TestFixtures.createUserFixtures(userService)
        val creds = TestFixtures.rawCredentialsFor(de.seuhd.campuscoffee.domain.model.objects.Role.MODERATOR)
        val posToCreate = TestFixtures.getPosFixturesForInsertion().first()
        val createdPos = de.seuhd.campuscoffee.tests.SystemTestUtils.withCredentials(creds.first, creds.second) {
            posDtoMapper.toDomain(
                posRequests.create(listOf(posDtoMapper.fromDomain(posToCreate))).first()
            )
        }

        assertEqualsIgnoringIdAndTimestamps(createdPos, posToCreate)
    }

    @Test
    fun `creating a POS returns a Location header pointing at the new resource`() {
        TestFixtures.createUserFixtures(userService)
        val creds = TestFixtures.rawCredentialsFor(de.seuhd.campuscoffee.domain.model.objects.Role.MODERATOR)
        val dto = posDtoMapper.fromDomain(TestFixtures.getPosFixturesForInsertion().first())

        val result = de.seuhd.campuscoffee.tests.SystemTestUtils.withCredentials(creds.first, creds.second) {
            client()
                .post()
                .uri("/api/pos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .withAuth()
                .exchange()
                .returnResult<PosDto>()
        }

        assertThat(result.status.value()).isEqualTo(HttpStatus.CREATED.value())
        assertThat(result.responseHeaders.location.toString()).endsWith("/api/pos/${result.responseBody!!.id}")
    }

    @Test
    fun `listing all POS returns every created entry`() {
        val createdPosList = TestFixtures.createPosFixtures(posService)

        val retrievedPos = posRequests.retrieveAll().map(posDtoMapper::toDomain)

        assertEqualsIgnoringTimestamps(retrievedPos, createdPosList)
    }

    @Test
    fun `fetching a POS by id returns it`() {
        val createdPos = TestFixtures.createPosFixtures(posService).first()

        val retrievedPos = posDtoMapper.toDomain(posRequests.retrieveById(createdPos.id!!))

        assertEqualsIgnoringTimestamps(retrievedPos, createdPos)
    }

    @Test
    fun `filtering POS by name returns the matching POS`() {
        val createdPos = TestFixtures.createPosFixtures(posService).first()
        val filteredPos = posDtoMapper.toDomain(posRequests.retrieveByFilter("name", createdPos.name))

        assertEqualsIgnoringTimestamps(filteredPos, createdPos)
    }

    @Test
    fun `updating a POS changes its fields and persists them`() {
        TestFixtures.createUserFixtures(userService)
        val creds = TestFixtures.rawCredentialsFor(de.seuhd.campuscoffee.domain.model.objects.Role.MODERATOR)
        val original = TestFixtures.createPosFixtures(posService).first()

        // domain models are immutable, so derive the updated instance with copy()
        val posToUpdate = original.copy(name = original.name + " (Updated)", description = "Updated description")

        val updatedPos = de.seuhd.campuscoffee.tests.SystemTestUtils.withCredentials(creds.first, creds.second) {
            posDtoMapper.toDomain(
                posRequests.update(listOf(posDtoMapper.fromDomain(posToUpdate))).first()
            )
        }
        assertEqualsIgnoringTimestamps(updatedPos, posToUpdate)

        // verify changes persist
        val retrievedPos = posDtoMapper.toDomain(posRequests.retrieveById(posToUpdate.id!!))
        assertEqualsIgnoringTimestamps(retrievedPos, posToUpdate)
    }

    @Test
    fun `deleting a POS twice returns 204 No Content then 404 Not Found`() {
        TestFixtures.createUserFixtures(userService)
        val creds = TestFixtures.rawCredentialsFor(de.seuhd.campuscoffee.domain.model.objects.Role.MODERATOR)
        val posToDelete = TestFixtures.createPosFixtures(posService).first()
        val id = requireNotNull(posToDelete.id)

        val statusCodes = de.seuhd.campuscoffee.tests.SystemTestUtils.withCredentials(creds.first, creds.second) {
            posRequests.deleteAndReturnStatusCodes(listOf(id, id))
        }

        // the first deletion returns 204 No Content, the second 404 Not Found
        assertThat(statusCodes).containsExactly(HttpStatus.NO_CONTENT.value(), HttpStatus.NOT_FOUND.value())

        val remainingPosIds: List<Long?> = posRequests.retrieveAll().map { it.id }
        assertThat(remainingPosIds).doesNotContain(id)
    }

    @Test
    fun `the API serves JSON even when the client prefers XML`() {
        TestFixtures.createPosFixtures(posService)

        // a browser prefers XML (application/xml;q=0.9) but also accepts */*; the API must answer JSON
        val result =
            client()
                .get()
                .uri("/api/pos")
                .accept(MediaType.APPLICATION_XML, MediaType.ALL)
                .exchange()
                .returnResult<String>()

        assertThat(result.status.value()).isEqualTo(HttpStatus.OK.value())
        assertThat(result.responseBody).startsWith("[") // a JSON array, not <List>/<PosDto> XML
    }
}
