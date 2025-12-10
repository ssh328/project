const imageModal = document.getElementById('imageModal');

if (imageModal) {
  imageModal.addEventListener('show.bs.modal', () => {
    imageModal.removeAttribute('inert');
  });

  imageModal.addEventListener('hide.bs.modal', () => {
    imageModal.setAttribute('inert', '');
  });
}