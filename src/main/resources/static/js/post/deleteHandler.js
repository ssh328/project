/**
 * 게시글 삭제 기능 초기화
 * DOM 로드 완료 후 삭제 모달과 버튼에 이벤트 리스너를 등록
 */
document.addEventListener('DOMContentLoaded', function() {
    // 삭제 모달과 삭제 버튼 요소 선택
    const deleteModal = document.getElementById('deleteModal');
    const deleteButton = document.getElementById('deleteButton');

    if (deleteModal && deleteButton) {
        // 모달이 열릴 때 삭제 버튼 상태 초기화
        deleteModal.addEventListener('shown.bs.modal', function() {
            deleteButton.disabled = false; // 모달 열릴 때 버튼 활성화
            deleteButton.innerHTML = '삭제하기'; // 초기 텍스트 복원
        });
    
        /**
         * 삭제 버튼 클릭 이벤트 핸들러
         * 게시글 삭제 요청을 처리하고 UI 상태를 관리
         * 버튼에 로딩 스피너 표시
         * 서버에 DELETE 요청 전송
         * 성공 시 게시글 목록 페이지로 이동
         * 실패 시 에러 메시지 표시 및 버튼 복원
         */
        deleteButton.addEventListener('click', async function() {
            // data 속성에서 게시글 ID 추출
            const postId = deleteButton.getAttribute('data-post-id');
            if (!postId) {
                console.error('게시글 ID를 찾을 수 없습니다.');
                alert('삭제할 게시글을 찾을 수 없습니다.');
                return;
            }

            // 버튼 너비 저장 (스피너 표시 시 너비 유지)
            const originalWidth = deleteButton.offsetWidth;
            
            // 버튼 비활성화 및 로딩 스피너 표시
            deleteButton.innerHTML = `
                <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
                <span class="visually-hidden" role="status">Loading...</span>
            `;
            deleteButton.style.width = originalWidth + 'px'; // 너비 유지
            deleteButton.disabled = true;

            // CSRF 토큰 및 헤더 이름 가져오기
            const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="csrf-header"]')?.getAttribute('content');
            
            try {
                // 게시글 삭제 API 요청
                const response = await fetch(`/post/delete?id=${postId}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        [csrfHeader]: csrfToken  // CSRF 토큰을 동적 헤더 이름으로 설정
                    }
                });

                // HTTP 응답 상태 확인
                if (!response.ok) {
                    const message = await response.text();
                    throw new Error(message || '삭제 요청 실패');
                }

                // 삭제 성공 시 게시글 목록 페이지로 이동
                location.href = '/post/list';
            } catch (err) {
                console.error('삭제 실패:', err);
                alert(err.message || '삭제 중 오류가 발생했습니다.');
                // 에러 발생 시 버튼 상태 복원
                deleteButton.innerHTML = '삭제하기';
                deleteButton.disabled = false;
            }
        });
    }
});
