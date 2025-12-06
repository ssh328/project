const fileInput = document.getElementById('formFileMultiple');
const fileNameInput = document.getElementById('fileNameInput');
const selectFilesButton = document.getElementById('selectFilesButton');
const fileCountMessage = document.getElementById('fileCountMessage');
const saveButton = document.getElementById('saveButton');
const deletedImagesInput = document.getElementById('deletedImages');
const fileInvalidFeedback = document.getElementById('file-invalid-feedback');
const form = document.getElementById('editForm');
const maxFiles = 10;

let selectedFiles = [];
let deletedImages = [];

// 기존 이미지 개수
let existingCount = existingImages.length;
fileCountMessage.textContent = `선택된 파일: ${existingCount}개 / 최대 ${maxFiles}`;


// ✅ 파일 유효성 검사 함수
function validateFiles() {
    const totalCount = existingCount + selectedFiles.length;

    if (totalCount === 0) {
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

// 파일 선택 이벤트
selectFilesButton.addEventListener('click', () => {
    fileInput.click();
});

fileInput.addEventListener('change', (e) => {
    // 새로 선택한 파일
    const newFiles = Array.from(fileInput.files);

    // 기존 선택 파일 + 새로 선택한 파일 합치기
    selectedFiles = [...selectedFiles, ...newFiles];

    // 최대 개수 제한
    if (selectedFiles.length + existingCount > maxFiles) {
        alert(`최대 ${maxFiles}개의 파일만 선택할 수 있습니다.`);
        selectedFiles = selectedFiles.slice(0, maxFiles - existingCount);
    }

    // 파일명 갱신
    updateFileInfo();

    // 새로 선택한 파일만 미리보기 추가
    previewSelectedImages(newFiles);

    validateFiles();

    // input 초기화 (같은 파일 재선택 가능)
    fileInput.value = '';
});

// 기존 이미지 삭제 버튼 이벤트
document.querySelectorAll('#imagePreviewContainer .btn-close').forEach(btn => {
    btn.addEventListener('click', () => {
        const previewDiv = btn.parentElement;
        // 기존 이미지 id를 가져와서 서버에서 삭제시키자.
        const imageId = btn.getAttribute('data-id');

        previewDiv.remove();
        deletedImages.push(imageId);
        deletedImagesInput.value = deletedImages.join(',');
        console.log("삭제 이미지" + deletedImages);

        existingCount--;
        updateFileInfo();
        validateFiles();
    });
});

// 새 이미지 미리보기 함수
function previewSelectedImages(files) {
    files.forEach(file => {
        const reader = new FileReader();
        reader.onload = (e) => {
            const previewDiv = document.createElement('div');
            previewDiv.classList.add('position-relative', 'd-inline-block');
            previewDiv.style.flex = '0 0 auto';

            previewDiv.innerHTML = `
                <img src="${e.target.result}"
                    alt="미리보기"
                    class="img-thumbnail rounded shadow-sm"
                    style="height: 180px; width: auto; object-fit: contain; flex-shrink: 0;">
                <button type="button" class="btn-close position-absolute"
                        style="top: 6px; right: 6px; width: 26px; height: 26px; z-index: 10;"
                        aria-label="이미지 삭제"></button>
            `;

            // 삭제 버튼 클릭 시 미리보기에서 제거
            previewDiv.querySelector('.btn-close').addEventListener('click', () => {
                previewDiv.remove();
                selectedFiles = selectedFiles.filter(f => f !== file);
                updateFileInfo();
                // 삭제 후 selectedFiles 상태 확인
                console.log("현재 selectedFiles:", selectedFiles);
                validateFiles();
            });

            imagePreviewContainer.appendChild(previewDiv);
        };
        reader.readAsDataURL(file);
    });
}

// 5️⃣ 파일 정보 갱신
function updateFileInfo() {
    fileNameInput.value = selectedFiles.map(f => f.name).join(', ') || '선택된 파일 없음';
    fileCountMessage.textContent = `선택된 파일: ${existingCount + selectedFiles.length}개 / 최대 ${maxFiles}`;
    // 상태 확인
    console.log("updateFileInfo 호출 - selectedFiles:", selectedFiles);
}

// 수정 과정
saveButton.addEventListener('click', async (e) => {
    const csrfToken = document.querySelector('meta[name="csrf-token"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="csrf-header"]').getAttribute('content');
    e.preventDefault();

    let isValid = true;

    if (!validateFiles()) {
        isValid = false;
    }

    // 부트스트랩 기본 폼 유효성 검사
    if (!form.checkValidity()) {
        form.classList.add('was-validated');
        isValid = false;
    }

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

    try {
        if (deletedImages.length > 0) {
            for (const id of deletedImages) {
                const deleteResponse = await fetch('/post/delete-image?imageId=' + id, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        [csrfHeader]: csrfToken
                    }
                });
            if (!deleteResponse.ok) {
                throw new Error('이미지 삭제 실패');
            }
            console.log("삭제된 이미지: ", deletedImages);
            }
        }

        const uploadedUrls = [];

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

        document.getElementById('deletedImages').value = deletedImages.join(',');

        console.log("폼에 세팅된 image:", document.getElementById('image-url').value);
        form.submit();
    } catch (err) {
        alert(err.message);

        // ✅ 로딩 복구
        saveButton.innerHTML = originalHTML;
        saveButton.disabled = false;
        saveButton.style.width = 'auto';
        return;
    }
});