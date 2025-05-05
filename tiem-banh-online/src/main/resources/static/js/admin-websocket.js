document.addEventListener('DOMContentLoaded', function () {
    connectWebSocket();
});

function connectWebSocket() {
    const socket = new SockJS('/ws'); // Endpoint đã cấu hình trong WebSocketConfig
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected to WebSocket: ' + frame);

        // Lắng nghe trên topic mà backend gửi đến
        stompClient.subscribe('/topic/admin/new-orders', function (message) {
            console.log('Received new order notification:', message.body);
            try {
                const notification = JSON.parse(message.body);
                showNewOrderNotification(notification);
            } catch (e) {
                console.error('Error parsing WebSocket message:', e);
            }
        });
    }, function(error) {
        // Xử lý lỗi kết nối, thử kết nối lại sau một khoảng thời gian
        console.error('STOMP error: ' + error);
        setTimeout(connectWebSocket, 5000); // Thử kết nối lại sau 5 giây
    });
}

function showNewOrderNotification(notification) {
    // Cách 1: Hiển thị Toast Bootstrap
    const toastContainer = document.getElementById('toastPlacement'); // Cần tạo thẻ div này trong layout admin
    if (toastContainer) {
         const toastId = 'toast-' + new Date().getTime(); // ID duy nhất
         const toastHTML = `
            <div id="${toastId}" class="toast align-items-center text-bg-info border-0" role="alert" aria-live="assertive" aria-atomic="true">
              <div class="d-flex">
                <div class="toast-body">
                  <i class="bi bi-receipt me-2"></i>
                  Đơn hàng mới <strong>#${notification.orderCode}</strong> từ ${notification.customerName} (${formatCurrency(notification.totalAmount)}).
                  <a href="/admin/orders/${notification.orderId}" class="ms-2 fw-bold text-white">Xem</a>
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
              </div>
            </div>`;
        toastContainer.insertAdjacentHTML('beforeend', toastHTML);
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, { delay: 10000 }); // Hiển thị 10s
        toast.show();
         // Tự động xóa toast khỏi DOM sau khi ẩn để tránh tích tụ
        toastElement.addEventListener('hidden.bs.toast', () => toastElement.remove());
    } else {
        // Cách 2: Alert đơn giản (nếu không có container toast)
         alert(`Đơn hàng mới #${notification.orderCode} từ ${notification.customerName}`);
    }


    // Cách 3: Cập nhật badge trên menu (ví dụ)
    const orderMenuBadge = document.getElementById('order-menu-badge');
    if (orderMenuBadge) {
        let currentCount = parseInt(orderMenuBadge.textContent || '0');
        orderMenuBadge.textContent = currentCount + 1;
        orderMenuBadge.style.display = 'inline-block'; // Hiện badge lên
    }

     // TODO: (Optional) Cân nhắc tự động refresh lại bảng đơn hàng nếu đang ở trang list order
     // if (window.location.pathname.includes('/admin/orders')) {
     //    console.log('Reloading orders list...');
          // Có thể dùng location.reload() hoặc fetch API để cập nhật bảng
     // }
}

 // Hàm helper định dạng tiền tệ
function formatCurrency(amount) {
    if (amount == null) return 'N/A';
    return amount.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' }).replace('₫',' ₫');
}

 // Thêm thẻ div này vào admin-layout.html, ví dụ gần header hoặc footer admin
 /*
 <div class="toast-container position-fixed bottom-0 end-0 p-3" id="toastPlacement" style="z-index: 1100">
     <!-- Toasts sẽ được thêm vào đây bằng JS -->
 </div>
 */
  // Thêm thẻ span này vào link Quản lý Đơn hàng trong sidebar
 /*
  <a class="nav-link ..." th:href="@{/admin/orders}" ...>
      <i class="bi bi-receipt"></i> Quản lý Đơn hàng
      <span id="order-menu-badge" class="badge bg-danger ms-auto" style="display: none;">0</span>
  </a>
 */