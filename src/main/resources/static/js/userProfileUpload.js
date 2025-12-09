document.getElementById('profile_edit_btn').addEventListener('click', () => {
    document.getElementById('file_input').click();
});

function toggleSubmitButton() {
    const fileInput = document.getElementById('file_input');
    const saveButton = document.getElementById('saveButton');
    const previewImg = document.getElementById('preview_img');
    const imageUrlInput = document.getElementById('image-url');
    const modalImg = document.querySelector('#imageModal img');
    const originalSrc = previewImg.dataset.originalSrc;
    
    saveButton.disabled = !fileInput.files.length
    saveButton.className = fileInput.files.length ? 'btn btn-warning ms-2' : 'btn btn-outline-warning ms-2'

    // 미리보기 이미지 업데이트
    if (fileInput.files.length) {
        const file = fileInput.files[0];
        const reader = new FileReader();
        reader.onload = function(e) {
            previewImg.src = e.target.result;
            modalImg.src = e.target.result;
            document.getElementById('image_preview').style.display = 'block';
        }
        reader.readAsDataURL(file);
    } else {
        previewImg.src = originalSrc; // current_user.profile_image_name으로 복원
        modalImg.src = originalSrc;
        document.getElementById('image_preview').style.display = 'block';   // 사진을 보이게 함
    }

    saveButton.addEventListener('click', async (e) => {
        e.preventDefault();

        const originalWidth = saveButton.offsetWidth;
        saveButton.innerHTML = `
            <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
            <span class="visually-hidden" role="status">Loading...</span>
        `;
        saveButton.style.width = originalWidth + 'px';
        saveButton.disabled = true;

        const uploadedUrls = [];
        try {
            const profileImage = fileInput.files[0];
            const fileName = profileImage.name;
            const response = await fetch('/presigned-url?filename=' + fileName);
            const result = await response.text();
            const uploadResponse = await fetch(result, {
                method: 'PUT',
                body: profileImage
            });
            if (uploadResponse.ok) {
                const imageUrl = result.split("?")[0];
                uploadedUrls.push(imageUrl);
            } else {
                alert('업로드 실패');
            }
        } catch (err) {
            alert('파일 업로드 중 오류가 발생했습니다.');
            console.error(err);
            return;
        }

        imageUrlInput.value = uploadedUrls[0];
        const form = document.getElementById('profileForm');
        form.submit();
        alert('업로드 완료');
    });
}