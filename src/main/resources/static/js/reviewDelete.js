document.querySelectorAll('.btn-close').forEach(btn => {
    btn.addEventListener('click', async () => {
        const reviewId = btn.getAttribute('data-id');

        try {
            const response = await fetch(`/review/${reviewId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            const message = await response.text();

            if (response.ok) {
                const reviewCard = btn.closest('.card');
                if (reviewCard) reviewCard.remove();
                alert(message);
            } else {
                alert(message);
            }
        } catch (error) {
            console.error('삭제 요청 중 에러 발생:', error);
            alert('서버 에러로 리뷰 삭제 못함');
        }
    });
});