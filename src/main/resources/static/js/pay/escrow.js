/**
 * 에스크로 주문 상세 페이지 관리
 * 주문 상태 조회, 배송 완료 처리, 구매 확정 처리 기능 제공
 */

const orderId = window.escrowOrderId;

const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="csrf-header"]')?.getAttribute('content');

const statusBadge = document.getElementById("statusBadge");
const deliverBtn = document.getElementById("deliverBtn");
const confirmBtn = document.getElementById("confirmBtn");
const hintText = document.getElementById("hintText");

const paidAtEl = document.getElementById("paidAt");
const deliveredAtEl = document.getElementById("deliveredAt");
const purchaseConfirmedAtEl = document.getElementById("purchaseConfirmedAt");
const settledAtEl = document.getElementById("settledAt");

/**
 * 날짜 시간 문자열을 포맷팅
 * @param {string} dateTimeStr - 포맷팅할 날짜 시간 문자열
 * @returns {string} 포맷팅된 날짜 시간 문자열
 */
function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) return "-";
  
  try {
    const date = new Date(dateTimeStr);
    if (isNaN(date.getTime())) return "-";
    
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${year}년 ${month}월 ${day}일 ${hours}시 ${minutes}분`;
  } catch (e) {
    console.error("날짜 포맷팅 실패:", e);
    return "-";
  }
}

/**
 * 주문 상태 조회 API 호출
 * @returns {Promise<Object>} 주문 정보 객체
 */
async function fetchOrder() {
  const res = await fetch(`/api/escrow/orders/${orderId}`);
  if (!res.ok) throw new Error("주문 조회 실패");
  return await res.json();
}

/**
 * 주문 상태를 UI에 적용
 * 상태 배지, 타임라인 날짜, 버튼 활성화 상태, 안내 문구를 업데이트
 * @param {Object} order - 주문 정보 객체
 */
function applyOrder(order) {
  
  // 상태 배지, 타임라인 날짜 표시
  if (statusBadge) statusBadge.textContent = order.statusDescription || order.status;
  if (paidAtEl) paidAtEl.textContent = formatDateTime(order.paidAt);
  if (deliveredAtEl) deliveredAtEl.textContent = formatDateTime(order.deliveredAt);
  if (purchaseConfirmedAtEl) purchaseConfirmedAtEl.textContent = formatDateTime(order.purchaseConfirmedAt);
  if (settledAtEl) settledAtEl.textContent = formatDateTime(order.settledAt);

  // 역할 판단 (버튼이 없을 수도 있으므로 옵셔널 체이닝 사용)
  const loginUserId = Number(deliverBtn?.dataset?.loginUserId || confirmBtn?.dataset?.loginUserId || 0);
  const sellerId = Number(deliverBtn?.dataset?.sellerId || 0);
  const buyerId = Number(confirmBtn?.dataset?.buyerId || 0);

  // 로그인한 사용자가 판매자인지 확인
  const isSeller = loginUserId && sellerId && loginUserId === sellerId;
  
  // 로그인한 사용자가 구매자인지 확인
  const isBuyer = loginUserId && buyerId && loginUserId === buyerId;

  // order.status를 정규화 (대소문자, 공백 제거)
  let statusValue = order.status;
  const statusStr = String(statusValue || "").trim().toUpperCase();

  // 배송완료 버튼: 판매자이고 PAYMENT_CONFIRMED 상태일 때만 활성화
  if (deliverBtn) {
    deliverBtn.disabled = !(isSeller && statusStr === "PAYMENT_CONFIRMED");
  }
  
  // 구매확정 버튼: 구매자이고 DELIVERY_MARKED 상태일 때만 활성화
  if (confirmBtn) {
    confirmBtn.disabled = !(isBuyer && statusStr === "DELIVERY_MARKED");
  }

  // 상태별 안내 문구 표시
  const hintElement = document.getElementById("hintText");
  if (hintElement) {

    hintElement.innerHTML = "";
    
    let hintMessage = "";
    if (statusStr === "PAYMENT_CONFIRMED") {
      hintMessage = "현재: 결제 승인 완료(에스크로 보관). 판매자가 배송완료 처리를 하면 구매확정이 가능합니다.";
    } else if (statusStr === "DELIVERY_MARKED") {
      hintMessage = "현재: 판매자 배송완료. 구매자가 구매확정을 누르면 정산(가상)이 진행됩니다.";
    } else if (statusStr === "SETTLED") {
      hintMessage = "현재: 정산 완료. (가상 정산)";
    } else if (statusStr === "PURCHASE_CONFIRMED") {
      hintMessage = "현재: 구매확정 완료. 정산 처리 중입니다.";
    } else if (statusStr === "CREATED") {
      hintMessage = "현재: 주문 생성됨. 결제 대기 중입니다.";
    } else {
      hintMessage = "";
    }
    
    // textContent로 설정 (innerHTML 대신 사용하여 XSS 방지)
    hintElement.textContent = hintMessage;
  }
}

/**
 * POST 요청을 보내는 공통 함수
 * @param {string} url - 요청할 URL
 * @returns {Promise<Object>} 응답 데이터
 */
async function postAction(url) {
  const res = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      [csrfHeader]: csrfToken,
    },
  });
  
  let body;
  try {
    body = await res.json();
  } catch (e) {
    throw new Error("응답 파싱 실패");
  }
  
  if (!res.ok) {
    throw new Error("요청 실패");
  }
  
  return body;
}

/**
 * 배송 완료 버튼 클릭 이벤트 핸들러
 * 판매자가 배송 완료 처리를 수행
 */
deliverBtn?.addEventListener("click", async () => {
  if (!confirm("배송완료 처리하시겠습니까?")) return;
  
  // 로딩 중 버튼 비활성화
  if (deliverBtn) deliverBtn.disabled = true;
  
  try {
    const order = await postAction(`/api/escrow/orders/${orderId}/deliver`);
    applyOrder(order);
  } catch (e) {
    alert(e.message);
    // 에러 발생 시에만 버튼 다시 활성화 (원래 상태로 복구)
    if (deliverBtn) deliverBtn.disabled = false;
  }
});

/**
 * 구매 확정 버튼 클릭 이벤트 핸들러
 * 구매자가 구매 확정을 수행하고 가상 정산이 진행
 */
confirmBtn?.addEventListener("click", async () => {
  if (!confirm("구매확정(에스크로 해제) 하시겠습니까?")) return;
  
  // 로딩 중 버튼 비활성화
  if (confirmBtn) confirmBtn.disabled = true;
  
  try {
    const order = await postAction(`/api/escrow/orders/${orderId}/confirm`);
    applyOrder(order);
  } catch (e) {
    alert(e.message);
    // 에러 발생 시에만 버튼 다시 활성화 (원래 상태로 복구)
    if (confirmBtn) confirmBtn.disabled = false;
  }
});

// 초기 로드 (DOM이 완전히 로드된 후 실행)
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}

/**
 * 초기화 함수
 * 주문 정보를 조회하고 UI에 적용
 */
async function init() {
  try {
    if (!orderId) {
      console.warn("orderId가 설정되지 않았습니다.");
      return;
    }
    const order = await fetchOrder();
    applyOrder(order);
  } catch (e) {
    console.error("주문 조회 실패:", e);
    alert("주문 정보를 불러올 수 없습니다: " + e.message);
  }
}