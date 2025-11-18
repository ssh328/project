document.addEventListener('DOMContentLoaded', function() {
    const deleteModal = document.getElementById('deleteModal');
    const deleteButton = document.getElementById('deleteButton');

    if (deleteModal && deleteButton) {
        deleteModal.addEventListener('shown.bs.modal', function() {
            deleteButton.disabled = false; // 모달 열릴 때 버튼 활성화
            deleteButton.innerHTML = '삭제하기'; // 초기 텍스트 복원
        });
    
        deleteButton.addEventListener('click', function() {
            const originalWidth = deleteButton.offsetWidth;
            
            // 버튼 비활성화 및 스피너 표시
            deleteButton.innerHTML = `
                <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
                <span class="visually-hidden" role="status">Loading...</span>
            `;
            deleteButton.style.width = originalWidth + 'px';
            deleteButton.disabled = true;
        });
    }
});
