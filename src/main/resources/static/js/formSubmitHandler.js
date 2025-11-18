// 유효성 검사 후 로딩 중 표시
document.querySelector('form').addEventListener('submit', function(event) {
    const saveButton = document.getElementById('saveButton');
    const originalWidth = saveButton.offsetWidth; // 원래 너비 저장
    
    if (!this.checkValidity()) {
        // 폼의 유효성을 검사, 유효하지 않으면 true 반환
        event.preventDefault(); // 유효성 검사 실패 시 제출 방지
        this.classList.add('was-validated'); // 유효성 검사 스타일 추가
        return;
    }
    
    saveButton.innerHTML = `
            <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
            <span class="visually-hidden" role="status">Loading...</span>
    `;
    saveButton.style.width = originalWidth + 'px'; // 원래 너비로 설정
    saveButton.disabled = true; // 버튼 비활성화
});