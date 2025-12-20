// DOM 요소 선택
const fileInput = document.getElementById('formFileMultiple');
const fileNameInput = document.getElementById('fileNameInput');
const selectFilesButton = document.getElementById('selectFilesButton');
const fileCountMessage = document.getElementById('fileCountMessage');
const saveButton = document.getElementById('saveButton');
const deletedImagesInput = document.getElementById('deletedImages');
const fileInvalidFeedback = document.getElementById('file-invalid-feedback');
const form = document.getElementById('editForm');
const maxFiles = 10; // 최대 파일 선택 개수

// 상태 관리 변수
let selectedFiles = []; // 새로 선택한 파일 목록
let deletedImages = []; // 삭제할 기존 이미지 ID 목록

// 기존 이미지 개수 초기화
let existingCount = existingImages.length;
fileCountMessage.textContent = `선택된 파일: ${existingCount}개 / 최대 ${maxFiles}`;

/**
 * 파일 유효성 검사
 * 기존 이미지와 새로 선택한 파일의 총 개수가 0인지 확인
 * @returns {boolean} 유효하면 true, 그렇지 않으면 false
 */
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

// 파일 선택 버튼 클릭 시 파일 입력창 열기
selectFilesButton.addEventListener('click', () => {
    fileInput.click();
});

/**
 * 파일 선택 변경 이벤트 핸들러
 * 새로 선택한 파일을 기존 목록에 추가하고 미리보기를 생성
 */
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

// 기존 이미지 삭제 버튼 이벤트 리스너 등록
document.querySelectorAll('#imagePreviewContainer .btn-close').forEach(btn => {
    btn.addEventListener('click', () => {
        const previewDiv = btn.parentElement;
        // data-id 속성에서 기존 이미지 ID 추출
        const imageId = btn.getAttribute('data-id');

        // 미리보기에서 제거
        previewDiv.remove();

        // 삭제 목록에 추가
        deletedImages.push(imageId);
        deletedImagesInput.value = deletedImages.join(',');
        console.log("삭제 이미지" + deletedImages);

        // 기존 이미지 개수 감소 및 UI 업데이트
        existingCount--;
        updateFileInfo();
        validateFiles();
    });
});

/**
 * 선택한 파일들의 미리보기 생성
 * FileReader를 사용하여 각 파일을 읽고 미리보기 이미지를 생성
 * @param {File[]} files - 미리보기를 생성할 파일 배열
 */
function previewSelectedImages(files) {
    files.forEach(file => {
        const reader = new FileReader();
        reader.onload = (e) => {
            // 미리보기 컨테이너 div 생성
            const previewDiv = document.createElement('div');
            previewDiv.classList.add('position-relative', 'd-inline-block');
            previewDiv.style.flex = '0 0 auto';

            // 이미지와 삭제 버튼이 포함된 HTML 생성
            previewDiv.innerHTML = `
                <img src="${e.target.result}"
                    alt="미리보기"
                    class="img-thumbnail rounded shadow-sm"
                    style="height: 180px; width: auto; object-fit: contain; flex-shrink: 0;">
                <button type="button" class="btn-close position-absolute"
                        style="top: 6px; right: 6px; width: 26px; height: 26px; z-index: 10;"
                        aria-label="이미지 삭제"></button>
            `;

            // 삭제 버튼 클릭 시 미리보기에서 제거 및 selectedFiles에서도 제거
            previewDiv.querySelector('.btn-close').addEventListener('click', () => {
                previewDiv.remove();
                selectedFiles = selectedFiles.filter(f => f !== file);
                updateFileInfo();
                console.log("현재 selectedFiles:", selectedFiles);
                validateFiles();
            });

            // 미리보기 컨테이너에 추가
            imagePreviewContainer.appendChild(previewDiv);
        };
        // 파일을 Data URL로 읽기
        reader.readAsDataURL(file);
    });
}

/**
 * 파일 정보 UI 업데이트
 * 선택한 파일명과 파일 개수를 화면에 표시
 */
function updateFileInfo() {
    // 파일명 입력란에 선택한 파일명 목록 표시
    fileNameInput.value = selectedFiles.map(f => f.name).join(', ') || '선택된 파일 없음';

    // 파일 개수 메시지 업데이트 (기존 + 새로 선택한 파일)
    fileCountMessage.textContent = `선택된 파일: ${existingCount + selectedFiles.length}개 / 최대 ${maxFiles}`;
}

/**
 * 게시글 수정 폼 제출 이벤트 핸들러
 * 폼 유효성 검사 후 다음 작업을 수행
 * 1. 삭제할 기존 이미지 서버에서 삭제
 * 2. 새로 선택한 파일을 S3에 업로드 (Presigned URL 사용)
 * 3. 업로드된 이미지 URL과 삭제된 이미지 ID를 폼에 설정
 * 4. 폼 제출
 * @param {Event} e - 클릭 이벤트 객체
 */
saveButton.addEventListener('click', async (e) => {
    // CSRF 토큰 및 헤더 이름 가져오기
    const csrfToken = document.querySelector('meta[name="csrf-token"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="csrf-header"]').getAttribute('content');
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

    try {
        // 1단계: 삭제할 기존 이미지 서버에서 삭제
        if (deletedImages.length > 0) {
            for (const id of deletedImages) {
                const deleteResponse = await fetch('/delete-image?imageId=' + id, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        [csrfHeader]: csrfToken  // CSRF 토큰을 동적 헤더 이름으로 설정
                    }
                });
                if (!deleteResponse.ok) {
                throw new Error('이미지 삭제 실패');
            }
            console.log("삭제된 이미지: ", deletedImages);
            }
        }

        // 2단계: 새로 선택한 파일들을 S3에 업로드
        const uploadedUrls = [];

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

        console.log("업로드 완료 후 URL들:", uploadedUrls);
        document.getElementById('image-url').value = uploadedUrls.join(',');

        document.getElementById('deletedImages').value = deletedImages.join(',');

        console.log("폼에 세팅된 image:", document.getElementById('image-url').value);
        form.submit();
    } catch (err) {
        alert(err.message);

        // 에러 발생 시 버튼 상태 복원
        saveButton.innerHTML = originalHTML;
        saveButton.disabled = false;
        saveButton.style.width = 'auto';
        return;
    }
});