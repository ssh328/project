function initReviewDelete(options) {
    const {
        selector,
        endpoint,
        dataAttribute,
        onSuccess,
        confirmMessage,
        errorMessage = '서버 에러로 리뷰 삭제 못함'
    } = options;

    const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="csrf-header"]')?.getAttribute('content');

    document.querySelectorAll(selector).forEach(btn => {
        btn.addEventListener('click', async function() {
            // 확인 메시지가 있으면 확인
            if (confirmMessage && !confirm(confirmMessage)) {
                return;
            }

            const reviewId = this.getAttribute(dataAttribute);
            if (!reviewId) {
                console.error('리뷰 ID를 찾을 수 없습니다.');
                return;
            }

            // endpoint가 함수면 함수 호출, 아니면 문자열 치환
            const url = typeof endpoint === 'function' 
                ? endpoint(reviewId, this) 
                : endpoint.replace('{id}', reviewId);

            try {
                const response = await fetch(url, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        [csrfHeader]: csrfToken
                    }
                });

                const message = await response.text();

                if (!response.ok) {
                    alert(message);
                    return;
                }

                if (onSuccess) {
                    onSuccess(reviewId, message, this);
                } else {
                    alert(message);
                }
            } catch (error) {
                console.error('삭제 요청 중 에러 발생:', error);
                alert(errorMessage);
            }
        });
    });
}
