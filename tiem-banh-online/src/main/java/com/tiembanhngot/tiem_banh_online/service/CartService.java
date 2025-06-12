package com.tiembanhngot.tiem_banh_online.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tiembanhngot.tiem_banh_online.dto.CartDTO;
import com.tiembanhngot.tiem_banh_online.dto.CartItemDTO;
import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; 


@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    @Autowired
    private static String CART_SESSION_KEY = "shoppingCart"; 
    @Autowired
    private ProductRepository productRepository;

    public CartDTO getCart(HttpSession session) {
        Object cartObject = session.getAttribute(CART_SESSION_KEY);
        CartDTO cart = null;

        if (cartObject instanceof CartDTO) {
            cart = (CartDTO) cartObject;
            log.trace("Cart found in session [ID: {}]. Items: {}", session.getId(), cart.getTotalItems());
            if (cart.getItems() == null) {
                log.warn("Cart found in session but items map is null. Initializing.");
                cart.setItems(new LinkedHashMap<>());
            }
            if (cart.getTotalAmount() == null) {
                 log.warn("Cart found in session but totalAmount is null. Initializing to ZERO.");
                 cart.setTotalAmount(BigDecimal.ZERO);
            }
        }

        if (cart == null) {
            log.info("No cart found in session [ID: {}]. Creating a new cart.", session.getId());
            cart = new CartDTO();
            cart.setItems(new LinkedHashMap<>());
            cart.setTotalAmount(BigDecimal.ZERO);
            cart.setTotalItems(0);
            session.setAttribute(CART_SESSION_KEY, cart);
            log.debug("New empty cart created and stored in session [ID: {}].", session.getId());
        }
        return cart;
    }

    @Transactional(readOnly = true)
    public void addToCart(Long productId, int quantity, String selectedSize, HttpSession session) {
        if (quantity <= 0) {
            log.warn("Attempted to add non-positive quantity ({}) for product ID: {}. Ignoring.", quantity, productId);
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }

        CartDTO cart = getCart(session);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));

        if (!Boolean.TRUE.equals(product.getIsAvailable())) {
             log.warn("Attempted to add unavailable product ID: {} ('{}') to cart.", productId, product.getName());
             throw new IllegalArgumentException("Product '" + product.getName() + "' is currently unavailable.");
        }
         if (product.getPrice() == null) {
              log.error("Product ID: {} ('{}') has a NULL price. Cannot add to cart.", productId, product.getName());
              throw new IllegalStateException("Product '" + product.getName() + "' has a pricing error. Please contact support.");
         }

        BigDecimal priceToUse = product.getPrice();
        String sizeIdentifier = selectedSize;

        if (StringUtils.hasText(selectedSize) && product.getSizeOptions() != null && !product.getSizeOptions().isEmpty()) {
            if (product.getSizeOptions().containsKey(selectedSize)) {
                priceToUse = product.getSizeOptions().get(selectedSize);
                log.debug("Using price for selected size '{}': {}", selectedSize, priceToUse);
                 if (priceToUse == null) {
                      log.error("Price for selected size '{}' of product ID {} is NULL.", selectedSize, productId);
                      throw new IllegalStateException("Product '" + product.getName() + "' has a pricing error for the selected size.");
                 }
            } else {
                log.warn("Invalid size '{}' selected for product ID {}. Using default price {}.", selectedSize, productId, product.getPrice());
                sizeIdentifier = null;
                priceToUse = product.getPrice();
                 if (priceToUse == null) {
                      log.error("Default price for product ID {} is NULL.", productId);
                      throw new IllegalStateException("Product '" + product.getName() + "' has a pricing error.");
                 }
            }
        } else {
             log.debug("No valid size selected or product has no size options. Using default price {}.", product.getPrice());
             sizeIdentifier = null;
             priceToUse = product.getPrice();
             if (priceToUse == null) {
                  log.error("Default price for product ID {} is NULL.", productId);
                  throw new IllegalStateException("Product '" + product.getName() + "' has a pricing error.");
             }
        }

        String itemKey = String.valueOf(productId); 
        if (StringUtils.hasText(sizeIdentifier)) { 
            itemKey += "_" + sizeIdentifier;
        }

        CartItemDTO existingItem = cart.getItems().get(itemKey);

        if (existingItem != null) {
            log.debug("Updating quantity for existing item with key {} (Product ID {}) in cart [Session: {}]. Old quantity: {}, Add quantity: {}",
                      itemKey, productId, session.getId(), existingItem.getQuantity(), quantity);
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            log.debug("Adding new item with key {} (Product ID {}) to cart [Session: {}] with quantity: {}",
                     itemKey, productId, session.getId(), quantity);
            CartItemDTO newItem = new CartItemDTO();
            newItem.setProductId(product.getProductId());
            newItem.setName(product.getName());
            newItem.setImageUrl(product.getImageUrl());
            newItem.setPrice(priceToUse);
            newItem.setQuantity(quantity);
            newItem.setSelectedSize(sizeIdentifier);
            cart.getItems().put(itemKey, newItem);
        }

        updateCartTotals(cart);

        log.info("Cart updated after adding/updating item with key {} (Product ID {}). Session ID: {}. Total items: {}, Total amount: {}",
                 itemKey, productId, session.getId(), cart.getTotalItems(), cart.getTotalAmount());
    }

    
    
    public void updateQuantity(Long productId, int quantity, String selectedSize, HttpSession session) {
        CartDTO cart = getCart(session);

        String itemKeyToFind = String.valueOf(productId);
        if (StringUtils.hasText(selectedSize)) {
            itemKeyToFind += "_" + selectedSize;
        }

        log.debug("Attempting to update quantity for item with key {} (Product ID: {}, Size: {}) to {} in cart [Session: {}].",
                  itemKeyToFind, productId, (selectedSize != null ? selectedSize : "N/A"), quantity, session.getId());

        CartItemDTO item = cart.getItems().get(itemKeyToFind);

        if (item != null) {
            if (quantity > 0) {
                log.debug("Updating quantity for item with key {} to {}.", itemKeyToFind, quantity);
                item.setQuantity(quantity);
            } else {
                log.debug("Quantity for item with key {} is {} (<= 0). Removing item from cart.", itemKeyToFind, quantity);
                cart.getItems().remove(itemKeyToFind);
            }
            updateCartTotals(cart);
            log.info("Cart updated after quantity change for item with key {}. Total items: {}, Total amount: {}",
                      itemKeyToFind, cart.getTotalItems(), cart.getTotalAmount());
        } else {
             log.warn("Attempted to update quantity for item with key {} (Product ID {}, Size: {}), but item was not found in cart [Session: {}].",
                      itemKeyToFind, productId, (selectedSize != null ? selectedSize : "N/A"), session.getId());
        }
    }

   
    public void removeItem(Long productId, HttpSession session) {
        CartDTO cart = getCart(session);
        log.debug("Attempting to remove product ID {} from cart [Session: {}].", productId, session.getId());

        CartItemDTO removedItem = cart.getItems().remove(productId);

        if (removedItem != null) {
            log.debug("Successfully removed product ID {} from cart.", productId);
            updateCartTotals(cart);  // xoa sp khoi cart va tinh lai tien
          
        } else {
            throw new IllegalArgumentException("Sản phẩm không tồn tại trong giỏ hàng để xóa.");
        }
    }

    
    public void clearCart(HttpSession session) {
        log.info("Attempting to clear cart for session ID: {}", session.getId());
        if (session.getAttribute(CART_SESSION_KEY) != null) {
            session.removeAttribute(CART_SESSION_KEY);
            log.info("Cart attribute '{}' removed successfully for session ID: {}.", CART_SESSION_KEY, session.getId());
          
            if (session.getAttribute(CART_SESSION_KEY) == null) {
                 log.debug("Confirmed: Cart attribute '{}' is null after removal.", CART_SESSION_KEY);
            } else {
                 log.error("CRITICAL: Cart attribute '{}' STILL EXISTS after attempting removal for session ID: {}!", CART_SESSION_KEY, session.getId());
            }
        } else {
            log.warn("No cart attribute '{}' found in session ID {} to clear.", CART_SESSION_KEY, session.getId());
        }
    }

  
     public int getCartItemCount(HttpSession session) {
         return getCart(session).getTotalItems();
     }

    public BigDecimal getCartTotal(HttpSession session) {
        return getCart(session).getTotalAmount();
    }


    private void updateCartTotals(CartDTO cart) {
        if (cart == null) {
            log.error("Cannot update totals for a null cart.");
            return;
        }
        log.debug("Recalculating totals for cart...");
        
        BigDecimal calculatedTotalAmount = BigDecimal.ZERO;
        int calculatedTotalItems = 0;

        if (cart.getItems() != null) {
            for (CartItemDTO item : cart.getItems().values()) {
                if (item != null && item.getPrice() != null && item.getQuantity() > 0) {
                    try {
                        BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())); // gia ca * so luong
                        item.setLineTotal(lineTotal);
                        calculatedTotalAmount = calculatedTotalAmount.add(lineTotal);
                        calculatedTotalItems += item.getQuantity();
                    } catch (ArithmeticException e) {
                         log.error("ArithmeticException for item productId: {}", item.getProductId(), e);
                         item.setLineTotal(BigDecimal.ZERO);
                    }
                } else {
                    log.warn("Skipping item total calculation due to invalid data: {}", item);
                    if (item != null) item.setLineTotal(BigDecimal.ZERO);
                }
            }
        } else {
            log.warn("Cart items map is null. Totals will be zero.");
        }
        cart.setTotalAmount(calculatedTotalAmount);
        cart.setTotalItems(calculatedTotalItems);
        log.info("Cart totals recalculated: Items={}, Amount={}", cart.getTotalItems(), cart.getTotalAmount());
    }
}