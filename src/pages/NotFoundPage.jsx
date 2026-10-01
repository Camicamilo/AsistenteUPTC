import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <main className="notfound">
      <h1>Página no encontrada</h1>
      <p>La dirección que buscas no existe.</p>
      <Link className="btn btn-primary" to="/">
        Volver al inicio
      </Link>
    </main>
  );
}
