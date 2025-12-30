/**
 * 관리자 게시물 삭제 기능
 * 관리자 페이지에서 게시물 삭제 버튼 클릭 시 처리
 */
const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="csrf-header"]')?.getAttribute('content');

document.querySelectorAll('.admin-delete-btn').forEach(btn => {
    btn.addEventListener('click', async function() {
        if (!confirm('정말 이 게시글을 삭제하시겠습니까?')) {
            return;
        }

        const postId = this.getAttribute('data-post-id');
        const page = this.getAttribute('data-page');
        const keyword = this.getAttribute('data-keyword');
        const keywordParam = keyword ? '&keyword=' + encodeURIComponent(keyword) : '';
        
        try {
            const response = await fetch(`/admin/posts/${postId}/delete?page=${page}${keywordParam}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                }
            });

            if (!response.ok) {
                throw new Error('삭제 요청 실패');
            }

            // 삭제 성공 시 페이지 리다이렉트
            const redirectUrl = `/admin/posts?page=${page}${keywordParam}`;
            window.location.href = redirectUrl;
        } catch (err) {
            console.error('삭제 실패:', err);
            alert('삭제 중 오류가 발생했습니다.');
        }
    });
});

