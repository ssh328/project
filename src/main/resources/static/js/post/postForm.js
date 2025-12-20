// DOM 요소 선택
const fileInput = document.getElementById('formFileMultiple');
const fileNameInput = document.getElementById('fileNameInput');
const selectFilesButton = document.getElementById('selectFilesButton');
const fileCountMessage = document.getElementById('fileCountMessage');
const saveButton = document.getElementById('saveButton');
const fileInvalidFeedback = document.getElementById('file-invalid-feedback');
const form = document.getElementById('postForm');
const maxFiles = 10; // 최대 파일 선택 개수

// 상태 관리 변수
let selectedFiles = []; // 선택한 파일 목록

/**
 * 파일 유효성 검사
 * 선택한 파일이 있는지 확인하고 UI에 유효성 상태 표시
 * @returns {boolean} 유효하면 true, 그렇지 않으면 false
 */
function validateFiles() {
    if (selectedFiles.length === 0) {
        fileInvalidFeedback.style.display = 'block';
        fileNameInput.classList.add('is-invalid');
        fileNameInput.classList.remove('is-valid');
        return false;
    } else {
        fileInvalidFeedback.style.display = 'none';
        fileNameInput.classList.remove('is-invalid');
        fileNameInput.classList.add('is-valid');
        return true;
    }
}

// 파일 선택 버튼 클릭 시 파일 입력창 열기
selectFilesButton.addEventListener('click', () => {
    fileInput.click();
})

/**
 * 파일 선택 변경 이벤트 핸들러
 * 선택한 파일을 처리하고 UI를 업데이트
 */
fileInput.addEventListener('change', (e) => {
    // 선택한 파일 배열로 변환
    selectedFiles = Array.from(fileInput.files);
    
    // 최대 개수 제한 확인
    if (selectedFiles.length > maxFiles) {
        alert(`최대 ${maxFiles}개의 파일만 선택할 수 있습니다.`);
        selectedFiles = selectedFiles.slice(0, maxFiles);
    }

    // 파일명 입력란과 파일 개수 메시지 업데이트
    fileNameInput.value = selectedFiles.map(f => f.name).join(', ') || '선택된 파일 없음';
    fileCountMessage.textContent = `선택된 파일: ${selectedFiles.length}개 / 최대 ${maxFiles}`;

    validateFiles();
});

/**
 * 게시글 작성 폼 제출 이벤트 핸들러
 * 폼 유효성 검사 후 다음 작업을 수행
 * 1. 선택한 파일들을 S3에 업로드 (Presigned URL 사용)
 * 2. 업로드된 이미지 URL을 폼에 설정
 * 3. 폼 제출
 * @param {Event} e - 클릭 이벤트 객체
 */
saveButton.addEventListener('click', async (e) => {
    e.preventDefault();

    // 폼 유효성 검사
    let isValid = true;

    if (!validateFiles()) {
        isValid = false;
    }

    // 부트스트랩 기본 폼 유효성 검사
    if (!form.checkValidity()) {
        form.classList.add('was-validated');
        isValid = false;
    }

    // 유효성 검사 실패 시 제출 중단
    if (!isValid) {
        e.stopPropagation();
        return;
    }

    // 버튼에 로딩 스피너 표시
    const originalWidth = saveButton.offsetWidth;
    saveButton.innerHTML = `
        <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        <span class="visually-hidden" role="status">Loading...</span>
    `;
    saveButton.style.width = originalWidth + 'px'; // 너비 유지
    saveButton.disabled = true;

    const uploadedUrls = [];
    try {
        // 선택한 파일들을 S3에 업로드
        for (const file of selectedFiles) {
            // Presigned URL 요청
            const fileName = encodeURIComponent(file.name);
            const response = await fetch('/presigned-url?filename=' + fileName);
            const result = await response.text();

            // Presigned URL을 사용하여 S3에 직접 업로드
            const uploadResponse = await fetch(result, {
                method: 'PUT',
                body: file
            });

            if (uploadResponse.ok) {
                // Presigned URL에서 쿼리 파라미터 제거하여 실제 이미지 URL 추출
                const imageUrl = result.split("?")[0];
                uploadedUrls.push(imageUrl);
            } else {
                alert('업로드 실패')
            }
        }

        // 폼에 업로드된 이미지 URL 설정
        console.log("업로드 완료 후 URL들:", uploadedUrls);
        document.getElementById('image-url').value = uploadedUrls.join(',');

        console.log("폼에 세팅된 image:", document.getElementById('image-url').value);
        // 폼 제출
        form.submit();

    } catch (err) {
        alert('파일 업로드 중 오류가 발생했습니다.')
        console.error(err);

        // 에러 발생 시 버튼 상태 복원
        saveButton.innerHTML = originalHTML;
        saveButton.disabled = false;
        saveButton.style.width = 'auto';
        return;
    }
});