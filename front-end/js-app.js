(function () {
  'use strict';
  function showToast(message) {
    var toast = document.querySelector('.toast');
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add('show');
    window.clearTimeout(window.__toastTimer);
    window.__toastTimer = window.setTimeout(function () { toast.classList.remove('show'); }, 3200);
  }
  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form[data-demo-form]').forEach(function (form) {
      form.addEventListener('submit', function (event) {
        event.preventDefault();
        showToast('Dados validados. A operação ficará disponível quando a API estiver conectada.');
        form.reset();
      });
    });
    document.querySelectorAll('[data-action="clear"]').forEach(function (button) {
      button.addEventListener('click', function () {
        var form = button.closest('form');
        if (form) form.reset();
        showToast('Formulário limpo.');
      });
    });
    document.querySelectorAll('[data-action="demo"]').forEach(function (element) {
      element.addEventListener('click', function (event) {
        event.preventDefault();
        showToast(element.dataset.message || 'Ação pronta para integração com a API.');
      });
    });
  });
}());
