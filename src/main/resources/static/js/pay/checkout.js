/**
 * Toss Payments 결제 위젯 초기화 및 결제 처리
 */

const payBtn = document.getElementById("payButton");

if (!window.tossClientKey) {
  alert("TOSS_CLIENT_KEY가 설정되지 않았습니다. (.env 또는 환경변수 확인)");
}

if (typeof TossPayments === "undefined") {
  alert("토스 결제위젯 스크립트를 로드하지 못했습니다. 네트워크/애드블록/콘솔 네트워크 탭을 확인해주세요.");
  throw new Error("TossPayments is not defined");
}

/**
 * Toss Payments 위젯 초기화 및 결제 버튼 이벤트 등록
 * 결제 완료 전까지는 DB에 주문을 저장하지 않고 클라이언트에서 orderId 생성
 */
(async function init() {
  // v2 표준: TossPayments(clientKey).widgets({ customerKey }) 사용
  const tossPayments = TossPayments(window.tossClientKey);
  const widgets = tossPayments.widgets({ customerKey: window.customerKey });

  // 금액 설정
  await widgets.setAmount({ currency: "KRW", value: window.amount });

  // 결제수단/약관 렌더링
  await widgets.renderPaymentMethods({ selector: "#payment-method", variantKey: "DEFAULT" });
  await widgets.renderAgreement({ selector: "#agreement", variantKey: "AGREEMENT" });

  /**
   * 결제 버튼 클릭 이벤트 핸들러
   * 클라이언트에서 orderId를 생성하고 Toss Payments 결제 요청
   */
  payBtn?.addEventListener("click", async () => {
    payBtn.disabled = true;

    try {
      // 클라이언트에서 임시 orderId 생성 (결제 완료 전까지는 DB에 저장하지 않음)
      const tempOrderId = crypto.randomUUID().replace(/-/g, '');

      // 결제 요청 (결제 완료 후 /pay/success에서 주문 생성)
      await widgets.requestPayment({
        orderId: tempOrderId,
        orderName: window.postTitle,
        successUrl: `${location.origin}/pay/success?postId=${window.postId}`,
        failUrl: `${location.origin}/pay/fail`,
        customerName: window.customerName,
      });
    } catch (e) {
      alert(e.message || "결제 요청에 실패했습니다.");
      payBtn.disabled = false;
    }
  });
})();



