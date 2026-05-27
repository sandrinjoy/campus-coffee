package de.seuhd.campuscoffee.data.implementations;

import de.seuhd.campuscoffee.data.mapper.PosEntityMapper;
import de.seuhd.campuscoffee.data.mapper.ReviewEntityMapper;
import de.seuhd.campuscoffee.data.mapper.UserEntityMapper;
import de.seuhd.campuscoffee.data.persistence.entities.ReviewEntity;
import de.seuhd.campuscoffee.data.persistence.repositories.ReviewRepository;
import de.seuhd.campuscoffee.domain.model.objects.Pos;
import de.seuhd.campuscoffee.domain.model.objects.Review;
import de.seuhd.campuscoffee.domain.model.objects.User;
import de.seuhd.campuscoffee.domain.ports.data.ReviewDataService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ReviewDataServiceImpl
        extends CrudDataServiceImpl<Review, ReviewEntity, ReviewRepository, Long>
        implements ReviewDataService {

    private final PosEntityMapper posEntityMapper;
    private final UserEntityMapper userEntityMapper;

    // reviews have no unique constraints, so no constraint mappings are declared
    ReviewDataServiceImpl(ReviewRepository repository, ReviewEntityMapper entityMapper,
                          PosEntityMapper posEntityMapper, UserEntityMapper userEntityMapper) {
        super(repository, entityMapper, Review.class, Set.of());
        this.posEntityMapper = posEntityMapper;
        this.userEntityMapper = userEntityMapper;
    }

    @Override
    public @NonNull List<Review> filter(@NonNull Pos pos, @NonNull Boolean approved) {
        return repository.findAllByPosAndApproved(posEntityMapper.toEntity(pos), approved)
                .stream()
                .map(mapper::fromEntity)
                .toList();
    }

    @Override
    public @NonNull List<Review> filter(@NonNull Pos pos, @NonNull User author) {
        return repository.findAllByPosAndAuthor(posEntityMapper.toEntity(pos), userEntityMapper.toEntity(author))
                .stream()
                .map(mapper::fromEntity)
                .toList();
    }
}
