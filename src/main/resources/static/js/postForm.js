const fileInput = document.getElementById('formFileMultiple');
const fileNameInput = document.getElementById('fileNameInput');
const selectFilesButton = document.getElementById('selectFilesButton');
const fileCountMessage = document.getElementById('fileCountMessage');
const saveButton = document.getElementById('saveButton');
const fileInvalidFeedback = document.getElementById('file-invalid-feedback');
const form = document.getElementById('postForm');
const maxFiles = 10;

let selectedFiles = [];

// ✅ 파일 유효성 검사 함수 (중복 제거)
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

// 파일 선택창 열기
selectFilesButton.addEventListener('click', () => {
    fileInput.click();
})

fileInput.addEventListener('change', (e) => {
    selectedFiles = Array.from(fileInput.files);
    if (selectedFiles.length > maxFiles) {
    alert(`최대 ${maxFiles}개의 파일만 선택할 수 있습니다.`);
    selectedFiles = selectedFiles.slice(0, maxFiles);
    }

    fileNameInput.value = selectedFiles.map(f => f.name).join(', ') || '선택된 파일 없음';
    fileCountMessage.textContent = `선택된 파일: ${selectedFiles.length}개 / 최대 ${maxFiles}`;

    validateFiles();
});

saveButton.addEventListener('click', async (e) => {
    e.preventDefault();

    let isValid = true; // 전체 유효성 통과 여부

    if (!validateFiles()) {
            isValid = false;
        }

    // ✅ (2) 부트스트랩 기본 폼 검사
    if (!form.checkValidity()) {
        form.classList.add('was-validated');
        isValid = false;
    }

    // ✅ 둘 중 하나라도 실패하면 제출 중단
    if (!isValid) {
        e.stopPropagation();
        return;
    }

    // 로딩 표시
    const originalWidth = saveButton.offsetWidth;
    saveButton.innerHTML = `
        <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        <span class="visually-hidden" role="status">Loading...</span>
    `;
    saveButton.style.width = originalWidth + 'px';
    saveButton.disabled = true;

    const uploadedUrls = [];
    try {
        for (const file of selectedFiles) {
            const fileName = encodeURIComponent(file.name);
            const response = await fetch('/post/presigned-url?filename=' + fileName);
            const result = await response.text();

            const uploadResponse = await fetch(result, {
                method: 'PUT',
                body: file
            });

            if (uploadResponse.ok) {
                const imageUrl = result.split("?")[0];
                uploadedUrls.push(imageUrl);
            } else {
                alert('업로드 실패')
            }
        }

        console.log("업로드 완료 후 URL들:", uploadedUrls);
        document.getElementById('image-url').value = uploadedUrls.join(',');

        console.log("폼에 세팅된 image:", document.getElementById('image-url').value);
        form.submit();

    } catch (err) {
        alert('파일 업로드 중 오류가 발생했습니다.')
        console.error(err);

        // ✅ 로딩 복구
        saveButton.innerHTML = originalHTML;
        saveButton.disabled = false;
        saveButton.style.width = 'auto';
        return;
    }
});