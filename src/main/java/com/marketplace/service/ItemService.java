package com.marketplace.service;

import com.marketplace.entity.Auction;
import com.marketplace.entity.Item;
import com.marketplace.entity.ItemImage;
import com.marketplace.entity.User;
import com.marketplace.enums.AuctionStatus;
import com.marketplace.enums.ItemStatus;
import com.marketplace.repository.AuctionRepository;
import com.marketplace.repository.ItemRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuctionRepository auctionRepository;

    @Transactional
    public Item createItem(Item item, MultipartFile image, String userEmail) {
        User seller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        item.setSeller(seller);
        item.setStatus(ItemStatus.ACTIVE);

        if (item.getSaleType() == null) {
            throw new RuntimeException("Sale type is required");
        }

        if (item.getSaleType() == com.marketplace.enums.SaleType.AUCTION) {
            if (item.getStartingBid() == null) {
                throw new RuntimeException("Starting bid is required for auctions");
            }
            item.setCurrentPrice(item.getStartingBid());
        } else if (item.getSaleType() == com.marketplace.enums.SaleType.FIXED_PRICE) {
            if (item.getFixedPrice() == null) {
                throw new RuntimeException("Fixed price is required for fixed price items");
            }
            item.setCurrentPrice(item.getFixedPrice());
        }

        // Handle Image Upload
        if (image != null && !image.isEmpty()) {
            String fileName = fileStorageService.storeFile(image);

            ItemImage itemImage = ItemImage.builder()
                    .item(item)
                    .imageUrl(fileName) // Store filename, we'll serve it via static resource handler
                    .isPrimary(true)
                    .displayOrder(0)
                    .build();

            if (item.getImages() == null) {
                item.setImages(new ArrayList<>());
            }
            item.getImages().add(itemImage);
        }

        Item savedItem = itemRepository.save(item);

        if (item.getSaleType() == com.marketplace.enums.SaleType.AUCTION) {
            Auction auction = Auction.builder()
                    .item(savedItem)
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().plusDays(7)) // Default 7 days
                    .minBidIncrement(new BigDecimal("1.00"))
                    .status(AuctionStatus.ACTIVE)
                    .build();
            auctionRepository.save(auction);
        }

        return savedItem;
    }

    public List<Item> getAllAvailableItems() {
        return itemRepository.findByStatus(ItemStatus.ACTIVE);
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));
    }

    public List<Item> getItemsBySeller(String userEmail) {
        User seller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return itemRepository.findBySeller(seller);
    }
}
