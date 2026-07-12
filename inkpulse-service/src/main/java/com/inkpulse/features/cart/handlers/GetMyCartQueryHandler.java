package com.inkpulse.features.cart.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.features.cart.queries.GetMyCartQuery;
import com.inkpulse.models.response.cart.CartItemResponse;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.entities.Cart;
import com.inkpulse.entities.CartItem;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Book;
import com.inkpulse.repositories.CartRepository;
import com.inkpulse.repositories.CartItemRepository;
import com.inkpulse.models.pagination.PagedList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetMyCartQueryHandler implements Query.QueryHandler<GetMyCartQuery, PagedList<CartItemResponse>> {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedList<CartItemResponse> handle(GetMyCartQuery query) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(query.getUserId());
        if (cartOpt.isEmpty()) {
            return new PagedList<>(Collections.emptyList(), 0, query.getPageNumber(), query.getPageSize());
        }

        Cart cart = cartOpt.get();
        Pageable pageable = query.toPageable();

        Page<CartItem> cartItemsPage = cartItemRepository.findAllByCartId(cart.getId(), pageable);

        return PagedList.fromPage(cartItemsPage, item -> {
            BookEdition edition = item.getEdition();
            Book book = edition.getBook();

            String authorNameJoined = "";
            if (book.getBookAuthors() != null) {
                authorNameJoined = book.getBookAuthors().stream()
                        .filter(ba -> ba.isActive() && ba.getAuthor() != null)
                        .map(ba -> ba.getAuthor().getName())
                        .collect(Collectors.joining(", "));
            }

            int stockQty = edition.getStockQuantity();
            boolean stockSufficient = item.getQuantity() <= stockQty;

            return new CartItemResponse(
                    item.getId().toString(),
                    edition.getId().toString(),
                    book.getTitle(),
                    authorNameJoined,
                    edition.getThumbnailUrl(),
                    edition.getPrice(),
                    BookEditionResponse.formatVnd(edition.getPrice()),
                    item.getQuantity(),
                    stockQty,
                    stockSufficient,
                    edition.getEditionNumber(),
                    edition.getCoverType() != null ? edition.getCoverType().name() : null,
                    edition.getIsbn()
            );
        });
    }
}
