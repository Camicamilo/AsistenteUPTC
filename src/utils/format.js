const nf = new Intl.NumberFormat('es-CO');
const nf1 = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1, minimumFractionDigits: 1 });

export const formatNumber = (n) => nf.format(Math.round(n ?? 0));
export const formatDecimal = (n) => nf1.format(n ?? 0);
export const formatPercent = (ratio) => `${nf1.format((ratio ?? 0) * 100)} %`;
export const formatSeconds = (ms) => `${nf1.format((ms ?? 0) / 1000)} s`;

export function formatCompact(n) {
  if (n >= 1_000_000) return `${nf1.format(n / 1_000_000)} M`;
  if (n >= 10_000) return `${nf1.format(n / 1000)} mil`;
  return nf.format(n);
}

export function formatTime(date) {
  return new Date(date).toLocaleTimeString('es-CO', { hour: 'numeric', minute: '2-digit' });
}

export function formatDateTime(date) {
  return new Date(date).toLocaleString('es-CO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

export function formatDay(date) {
  return new Date(date).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });
}

export function formatHour(h) {
  return `${String(h).padStart(2, '0')}:00`;
}
