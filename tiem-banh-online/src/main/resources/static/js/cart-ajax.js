document.addEventListener('DOMContentLoaded', function() {
    // -- Hàm cập nhật số lượng trên header --
    function updateCartHeaderCount(newCount) {
        const cartCountSpan = document.getElementById('cart-item-count-display');
        if (cartCountSpan) {
            if (newCount > 0) {
                cartCountSpan.textContent = newCount;
                cartCountSpan.style.display = ''; // Hiện span
            } else {
                cartCountSpan.textContent = '0';
                cartCountSpan.style.display = 'none'; // Ẩn span
            }
        }
    }

    // -- Xử lý Form Thêm vào giỏ (ví dụ cho nút trong list.html) --
    document.querySelectorAll('.add-to-cart-form').forEach(form => {
        form.addEventListener('submit', function(event) {
            event.preventDefault(); // Ngăn submit form mặc định

            const formData = new FormData(this);
            const csrfToken = formData.get('_csrf'); // Lấy token từ input ẩn
            const productId = formData.get('productId');
            const quantity = formData.get('quantity');

            fetch(this.action, { // Gửi request đến action của form (/cart/add)
                method: 'POST',
                headers: {
                    // Thêm CSRF header nếu backend yêu cầu (phổ biến hơn là gửi trong body)
                    // 'X-CSRF-TOKEN': csrfToken,
                    'Content-Type': 'application/x-www-form-urlencoded', // Hoặc application/json nếu gửi JSON
                },
                // Gửi dữ liệu form (đã bao gồm _csrf)
                body: new URLSearchParams(formData).toString()
            })
            .then(response => {
                if (!response.ok) {
                    // Xử lý lỗi HTTP (4xx, 5xx)
                    console.error('Error adding to cart:', response.statusText);
                    alert('Lỗi khi thêm vào giỏ hàng!'); // Thông báo đơn giản
                    // TODO: Hiển thị lỗi chi tiết hơn
                    return response.json().catch(() => ({})); // Cố gắng parse lỗi JSON nếu có
                }
                 // **QUAN TRỌNG:** Backend cần trả về JSON chứa số lượng item mới
                return response.json();
            })
            .then(data => {
                 if (data && typeof data.itemCount !== 'undefined') {
                     console.log('Cart updated. New item count:', data.itemCount);
                     updateCartHeaderCount(data.itemCount); // Cập nhật số trên header
                     // Hiển thị thông báo thành công (ví dụ: dùng Toast Bootstrap)
                     // const toast = new bootstrap.Toast(document.getElementById('cartSuccessToast'));
                     // toast.show();
                     alert('Đã thêm vào giỏ hàng!'); // Thông báo đơn giản
                 } else if (data && data.error) {
                     console.error('Error adding to cart:', data.error);
                     alert('Lỗi: ' + data.error);
                 } else {
                     // Response không hợp lệ từ backend
                     console.warn('Received unexpected response:', data);
                 }
            })
            .catch(error => {
                console.error('Fetch error:', error);
                alert('Đã xảy ra lỗi mạng khi thêm vào giỏ hàng.');
            });
        });
    });

    // -- TODO: Xử lý AJAX cho Update/Remove trong trang cart.html tương tự --
    // Gắn event listener vào input số lượng hoặc nút xóa
    // Gửi Fetch request đến /cart/update hoặc /cart/remove/{id}
    // Cập nhật lại dòng sản phẩm, tổng tiền, và số lượng trên header bằng JS
});