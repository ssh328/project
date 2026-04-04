/**
 * 관리자 게시물 삭제 기능
 * 관리자 페이지에서 게시물 삭제 버튼 클릭 시 처리
 */
const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="csrf-header"]')?.getAttribute('content');

async function submitAdminPostAction({ postId, page, keyword, method, action, successMessage, confirmMessage }) {
    const keywordParam = keyword ? '&keyword=' + encodeURIComponent(keyword) : '';

    if (confirmMessage && !confirm(confirmMessage)) {
        return;
    }

    try {
        const response = await fetch(`/admin/posts/${postId}/${action}?page=${page}${keywordParam}`, {
            method,
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                [csrfHeader]: csrfToken
            }
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || `${successMessage} 요청 실패`);
        }

        const redirectUrl = `/admin/posts?page=${page}${keywordParam}`;
        window.location.href = redirectUrl;
    } catch (err) {
        console.error(`${successMessage} 실패:`, err);
        alert(err.message || `${successMessage} 중 오류가 발생했습니다.`);
    }
}

document.querySelectorAll('.admin-delete-btn').forEach(btn => {
    btn.addEventListener('click', async function() {
        await submitAdminPostAction({
            postId: this.getAttribute('data-post-id'),
            page: this.getAttribute('data-page'),
            keyword: this.getAttribute('data-keyword'),
            method: 'DELETE',
            action: 'delete',
            successMessage: '삭제',
            confirmMessage: '정말 이 게시글을 삭제하시겠습니까?'
        });
    });
});

document.querySelectorAll('.admin-restore-btn').forEach(btn => {
    btn.addEventListener('click', async function() {
        await submitAdminPostAction({
            postId: this.getAttribute('data-post-id'),
            page: this.getAttribute('data-page'),
            keyword: this.getAttribute('data-keyword'),
            method: 'POST',
            action: 'restore',
            successMessage: '복구',
            confirmMessage: '이 게시글을 다시 복구하시겠습니까?'
        });
    });
});

