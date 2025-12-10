document.addEventListener('DOMContentLoaded', function() {
    const deleteModal = document.getElementById('deleteModal');
    const deleteButton = document.getElementById('deleteButton');

    if (deleteModal && deleteButton) {
        deleteModal.addEventListener('shown.bs.modal', function() {
            deleteButton.disabled = false; // 모달 열릴 때 버튼 활성화
            deleteButton.innerHTML = '삭제하기'; // 초기 텍스트 복원
        });
    
        deleteButton.addEventListener('click', function() {
            // postId 가져오기 (data 속성에서)
            const postId = deleteButton.getAttribute('data-post-id');
            if (!postId) {
                console.error('게시글 ID를 찾을 수 없습니다.');
                alert('삭제할 게시글을 찾을 수 없습니다.');
                return;
            }

            const originalWidth = deleteButton.offsetWidth;
            
            // 버튼 비활성화 및 스피너 표시
            deleteButton.innerHTML = `
                <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
                <span class="visually-hidden" role="status">Loading...</span>
            `;
            deleteButton.style.width = originalWidth + 'px';
            deleteButton.disabled = true;

            // CSRF 토큰 가져오기
            const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="csrf-header"]')?.getAttribute('content');
            
            // 실제 삭제 요청
            fetch(`/post/delete?id=${postId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                }
            })
            .then(r => {
                if (!r.ok) {
                    throw new Error('삭제 요청 실패');
                }
                return r.text();
            })
            .then(() => {
                location.href = '/post/list';
            })
            .catch(err => {
                console.error('삭제 실패:', err);
                alert('삭제 중 오류가 발생했습니다.');
                // 에러 발생 시 버튼 복원
                deleteButton.innerHTML = '삭제하기';
                deleteButton.disabled = false;
            });
        });
    }
});
