package de.seuhd.campuscoffee.api.controller

import de.seuhd.campuscoffee.api.dtos.ReviewDto
import de.seuhd.campuscoffee.api.mapper.DtoMapper
import de.seuhd.campuscoffee.api.mapper.ReviewDtoMapper
import de.seuhd.campuscoffee.api.openapi.CrudOperation
import de.seuhd.campuscoffee.api.openapi.Operation.CREATE
import de.seuhd.campuscoffee.api.openapi.Operation.DELETE
import de.seuhd.campuscoffee.api.openapi.Operation.FILTER
import de.seuhd.campuscoffee.api.openapi.Operation.GET_ALL
import de.seuhd.campuscoffee.api.openapi.Operation.GET_BY_ID
import de.seuhd.campuscoffee.api.openapi.Operation.UPDATE
import de.seuhd.campuscoffee.api.openapi.Resource.REVIEW
import de.seuhd.campuscoffee.domain.model.objects.Review
import de.seuhd.campuscoffee.domain.ports.api.CrudService
import de.seuhd.campuscoffee.domain.ports.api.ReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * Controller for handling reviews for POS, authored by users.
 */
@Tag(name = "Reviews", description = "Operations for managing reviews for points of sale.")
@Controller
@RequestMapping("/reviews")
class ReviewController(
    private val reviewService: ReviewService,
    private val reviewDtoMapper: ReviewDtoMapper,
    private val currentUserProvider: de.seuhd.campuscoffee.api.security.CurrentUserProvider
) : CrudController<Review, ReviewDto, Long>() {
    override fun service(): CrudService<Review, Long> = reviewService

    override fun mapper(): DtoMapper<Review, ReviewDto> = reviewDtoMapper

    @Operation
    @CrudOperation(operation = GET_ALL, resource = REVIEW)
    @GetMapping("")
    override fun getAll(): ResponseEntity<List<ReviewDto>> = super.getAll()

    @Operation
    @CrudOperation(operation = GET_BY_ID, resource = REVIEW)
    @GetMapping("/{id}")
    override fun getById(
        @Parameter(description = "Unique identifier of the review to retrieve.", required = true)
        @PathVariable id: Long
    ): ResponseEntity<ReviewDto> = super.getById(id)

    @Operation
    @CrudOperation(operation = CREATE, resource = REVIEW)
    @PostMapping("")
    override fun create(
        @Parameter(description = "Data of the review to create.", required = true)
        @RequestBody
        @Valid dto: ReviewDto
    ): ResponseEntity<ReviewDto> {
        require(dto.authorId == null) { "authorId must not be provided" }
        return super.create(dto)
    }

    @Operation
    @CrudOperation(operation = UPDATE, resource = REVIEW)
    @PutMapping("/{id}")
    override fun update(
        @Parameter(description = "Unique identifier of the review to update.", required = true)
        @PathVariable id: Long,
        @Parameter(description = "Data of the review to update.", required = true)
        @RequestBody
        @Valid dto: ReviewDto
    ): ResponseEntity<ReviewDto> {
        require(dto.authorId == null) { "authorId must not be provided" }
        require(id == dto.id) { "ID in path and body do not match." }
        val actingUser = currentUserProvider.currentUser()
        val domain = reviewDtoMapper.toDomain(dto)
        val updated = reviewService.upsert(domain, actingUser)
        return ResponseEntity.ok(reviewDtoMapper.fromDomain(updated))
    }

    @Operation
    @CrudOperation(operation = DELETE, resource = REVIEW)
    @DeleteMapping("/{id}")
    override fun delete(
        @Parameter(description = "Unique identifier of the review to delete.", required = true)
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val actingUser = currentUserProvider.currentUser()
        reviewService.delete(id, actingUser)
        return ResponseEntity.noContent().build()
    }

    @Operation
    @CrudOperation(operation = FILTER, resource = REVIEW)
    @GetMapping("/filter")
    fun filter(
        @Parameter(description = "Unique identifier of the POS to retrieve approved reviews for.", required = true)
        @RequestParam("pos_id") posId: Long,
        @Parameter(description = "The approval status of the reviews to retrieve.", required = true)
        @RequestParam("approved") approved: Boolean
    ): ResponseEntity<List<ReviewDto>> =
        ResponseEntity.ok(reviewService.filter(posId, approved).map { reviewDtoMapper.fromDomain(it) })

    @Operation(summary = "Approve a review by ID.", responses = [
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review approved"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    ])
    @PutMapping("/{id}/approve")
    fun approve(
        @Parameter(description = "Unique identifier of the review to approve.", required = true)
        @PathVariable id: Long
    ): ResponseEntity<ReviewDto> = ResponseEntity.ok(reviewDtoMapper.fromDomain(reviewService.approve(id, currentUserProvider.currentUser().id!!)))
}
