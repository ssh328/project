const deleteModal = document.getElementById('deleteModal')
const deleteInput = document.getElementById('deleteInput')

if (deleteModal) {
  deleteModal.addEventListener('shown.bs.modal', () => {
    if (deleteInput) {
      deleteInput.focus()
    }
  })

  deleteModal.addEventListener('show.bs.modal', () => {
    deleteModal.removeAttribute('inert')
  })

  deleteModal.addEventListener('hide.bs.modal', () => {
    deleteModal.setAttribute('inert', '')
  })
}

const imageModal = document.getElementById('imageModal');

if (imageModal) {
  imageModal.addEventListener('show.bs.modal', () => {
    imageModal.removeAttribute('inert');
  });

  imageModal.addEventListener('hide.bs.modal', () => {
    imageModal.setAttribute('inert', '');
  });
}