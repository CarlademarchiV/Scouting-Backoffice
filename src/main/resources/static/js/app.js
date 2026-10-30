// Comportamiento de los formularios (sin scripts en línea, compatible con la CSP).
document.addEventListener('DOMContentLoaded', () => {
  // Muestra día de semana o día de mes según la frecuencia.
  const frecuencia = document.getElementById('frecuencia');
  const toggle = () => {
    if (!frecuencia) return;
    const f = frecuencia.value;
    document.getElementById('grupo-dia-semana').hidden = !(f === 'SEMANAL' || f === 'QUINCENAL');
    document.getElementById('grupo-dia-mes').hidden = f !== 'MENSUAL';
  };
  if (frecuencia) { frecuencia.addEventListener('change', toggle); toggle(); }

  // Contador de caracteres del prompt.
  const prompt = document.getElementById('prompt');
  const contador = document.getElementById('contador-prompt');
  const contar = () => { if (prompt && contador) contador.textContent = prompt.value.length.toLocaleString('es-UY') + ' / 100.000'; };
  if (prompt) { prompt.addEventListener('input', contar); contar(); }

  // Confirmaciones: <form data-confirmar="texto">.
  document.querySelectorAll('form[data-confirmar]').forEach(form => {
    form.addEventListener('submit', ev => { if (!window.confirm(form.dataset.confirmar)) ev.preventDefault(); });
  });
});
