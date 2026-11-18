// ============================================================================
// DATOS DE DEMOSTRACIÓN
// Este contenido existe solo para probar el frontend sin backend. NO es
// información oficial: el enlace institucional debe reemplazar textos,
// correos y enlaces por los validados (RD-01, RD-03, RD-04) antes del piloto.
// ============================================================================

export const DEPENDENCIAS = {
  admisiones: {
    clave: 'admisiones',
    nombre: 'Dirección de Admisiones',
    correo: 'admisiones@uptc.edu.co',
    telefono: '',
    enlace: 'https://www.uptc.edu.co/sitio/portal/front/index.html',
    horario: 'Lunes a viernes, horario de oficina',
  },
  registro: {
    clave: 'registro',
    nombre: 'Registro y Control Académico',
    correo: 'registro.academico@uptc.edu.co',
    telefono: '',
    enlace: 'https://www.uptc.edu.co/sitio/portal/front/index.html',
    horario: 'Lunes a viernes, horario de oficina',
  },
  tecnologia: {
    clave: 'tecnologia',
    nombre: 'Línea de Atención – DTIC',
    correo: 'soporte.dtic@uptc.edu.co',
    telefono: '',
    enlace: 'https://www.uptc.edu.co/sitio/portal/sitios/universidad/rectoria/dtics/09_linea_aten.html',
    horario: 'Lunes a viernes, horario de oficina',
  },
  bienestar: {
    clave: 'bienestar',
    nombre: 'Bienestar Universitario',
    correo: 'bienestar@uptc.edu.co',
    telefono: '',
    enlace: 'https://www.uptc.edu.co/sitio/portal/front/index.html',
    horario: 'Lunes a viernes, horario de oficina',
  },
  academica: {
    clave: 'academica',
    nombre: 'Vicerrectoría Académica',
    correo: 'vicerrectoria.academica@uptc.edu.co',
    telefono: '',
    enlace: 'https://www.uptc.edu.co/sitio/portal/front/index.html',
    horario: 'Lunes a viernes, horario de oficina',
  },
  general: {
    clave: 'general',
    nombre: 'Atención al Ciudadano',
    correo: 'atencion.ciudadano@uptc.edu.co',
    telefono: '',
    enlace: 'https://www.uptc.edu.co/sitio/portal/sitios/universidad/rectoria/dtics/09_linea_aten.html',
    horario: 'Lunes a viernes, horario de oficina',
  },
};

// Temas de acceso rápido del widget (prototipo 6.2).
export const TEMAS_RAPIDOS = [
  { id: 'matriculas', etiqueta: 'Matrículas', consulta: '¿Cuándo son las matrículas?' },
  { id: 'admisiones', etiqueta: 'Admisiones', consulta: '¿Cómo me inscribo como aspirante?' },
  { id: 'calendario', etiqueta: 'Calendario académico', consulta: '¿Dónde veo el calendario académico?' },
  { id: 'correo', etiqueta: 'Correo institucional', consulta: '¿Cómo activo mi correo institucional?' },
  { id: 'tramites', etiqueta: 'Trámites', consulta: '¿Cómo pido un certificado de estudio?' },
];

// Intenciones con palabras clave (peso) para el motor PLN simulado.
// `social: true` = saludo / despedida / agradecimiento (solo ganan si no hay otra intención).
export const INTENCIONES_SEED = [
  {
    nombre: 'saludo',
    categoria: 'administrativo',
    descripcion: 'Saludos iniciales del estudiante.',
    dependencia: 'general',
    social: true,
    claves: { hola: 3, buenas: 2.5, buenos: 2.5, saludos: 3, hey: 2, buen: 1 },
    ejemplos: ['Hola', 'Buenas tardes', 'Buenos días, una pregunta'],
    respuestas: [
      '¡Hola! Soy el Asistente Virtual de la UPTC. Puedo orientarte sobre admisiones, matrículas, calendario académico, correo institucional y trámites. ¿En qué te ayudo?',
      '¡Hola! Cuéntame qué necesitas: admisiones, matrículas, calendario, correo institucional o algún trámite.',
    ],
  },
  {
    nombre: 'despedida',
    categoria: 'administrativo',
    descripcion: 'Cierre de la conversación.',
    dependencia: 'general',
    social: true,
    claves: { chao: 3, adios: 3, hasta: 1.5, luego: 1.5, nos: 0.5, vemos: 1.5, bye: 3 },
    ejemplos: ['Chao', 'Hasta luego', 'Adiós'],
    respuestas: ['¡Hasta pronto! Si te surge otra duda, aquí estaré a cualquier hora.'],
  },
  {
    nombre: 'agradecimiento',
    categoria: 'administrativo',
    descripcion: 'El estudiante agradece la respuesta.',
    dependencia: 'general',
    social: true,
    claves: { gracias: 3, agradezco: 3, listo: 1.5, perfecto: 1.5, genial: 1.5, vale: 1 },
    ejemplos: ['Gracias', 'Muchas gracias', 'Listo, perfecto'],
    respuestas: ['¡Con gusto! ¿Hay algo más en lo que te pueda ayudar?'],
  },
  {
    nombre: 'info_admisiones',
    categoria: 'administrativo',
    descripcion: 'Cómo inscribirse como aspirante a un programa de pregrado.',
    dependencia: 'admisiones',
    claves: { admision: 2.5, admisiones: 2.5, inscripcion: 2, inscribo: 2, inscribirme: 2, aspirante: 2.5, ingresar: 1, entrar: 1, pregrado: 1.5, carrera: 1, estudiar: 1 },
    ejemplos: ['¿Cómo me inscribo como aspirante?', 'Quiero entrar a estudiar a la UPTC', 'Proceso de admisión a pregrado'],
    respuestas: [
      'El proceso de admisión lo coordina la Dirección de Admisiones. En general: 1) revisa la oferta de programas y sus requisitos en el portal institucional, 2) diligencia la inscripción en línea dentro de las fechas del calendario de admisiones vigente, 3) paga el derecho de inscripción y 4) consulta los resultados en la fecha publicada. Más información: https://www.uptc.edu.co',
    ],
  },
  {
    nombre: 'requisitos_admision',
    categoria: 'administrativo',
    descripcion: 'Documentos y requisitos para ser admitido.',
    dependencia: 'admisiones',
    claves: { requisitos: 2.5, requisito: 2.5, documentos: 3, papeles: 3, admision: 1, icfes: 2.5, saber: 1, puntaje: 2, necesito: 0.5 },
    ejemplos: ['¿Qué requisitos piden para entrar?', '¿Qué papeles necesito para la admisión?', '¿Cuánto puntaje del ICFES piden?'],
    respuestas: [
      'Los requisitos dependen del programa y de la modalidad de ingreso. Normalmente se solicita el documento de identidad, el resultado de la prueba Saber 11 y los soportes que indique la convocatoria. Revisa la convocatoria vigente de la Dirección de Admisiones en https://www.uptc.edu.co antes de inscribirte.',
    ],
  },
  {
    nombre: 'resultados_admision',
    categoria: 'administrativo',
    descripcion: 'Consulta de resultados o estado del proceso de admisión.',
    dependencia: 'admisiones',
    claves: { resultados: 2.5, resultado: 2.5, admitido: 3, admitida: 3, quede: 2, pase: 1.5, estado: 1.5, lista: 1, admision: 1, consulto: 1, proceso: 1.5 },
    ejemplos: ['¿Cómo sé si quedé admitido?', 'Resultados de admisión', '¿Dónde consulto el estado de mi proceso?'],
    respuestas: [
      'Los resultados de admisión se publican en el portal institucional en la fecha fijada por el calendario de admisiones. Si fuiste admitido, allí mismo encontrarás las instrucciones para la matrícula de primer semestre.',
    ],
  },
  {
    nombre: 'consulta_matricula',
    categoria: 'académico',
    descripcion: 'Fechas y periodos de matrícula.',
    dependencia: 'registro',
    claves: { matricula: 1.5, matriculas: 1.5, matricularme: 1.5, fecha: 1.5, fechas: 1.5, cuando: 1, plazo: 1.5, abren: 1, periodo: 1, extemporanea: 2 },
    ejemplos: ['¿Cuándo son las matrículas?', 'Fecha de matrícula por favor', '¿Cuándo abren matrículas para el próximo semestre?'],
    respuestas: [
      'Las fechas de matrícula (ordinaria y extemporánea) se fijan cada semestre en el calendario académico aprobado por el Consejo Académico. Consúltalas en el portal institucional: https://www.uptc.edu.co. Te recomiendo matricularte dentro del periodo ordinario para evitar recargos.',
      'Cada semestre Registro y Control Académico publica el periodo de matrícula según el calendario académico vigente. Puedes verlo en https://www.uptc.edu.co.',
    ],
  },
  {
    nombre: 'pasos_matricula',
    categoria: 'académico',
    descripcion: 'Cómo realizar la matrícula financiera y académica.',
    dependencia: 'registro',
    claves: { matricula: 1.5, pagar: 2, pago: 2, recibo: 2.5, liquidacion: 2.5, pin: 2.5, pasos: 1.5, financiera: 2, academica: 1, hago: 1, hacer: 0.5 },
    ejemplos: ['¿Cómo hago la matrícula?', '¿Dónde descargo el recibo de pago?', 'Necesito el PIN para matricularme'],
    respuestas: [
      'La matrícula tiene dos partes: 1) matrícula financiera: descarga tu recibo de liquidación, págalo en los canales autorizados y espera su confirmación; 2) matrícula académica: inscribe tus asignaturas en la plataforma académica dentro de las fechas del calendario. Si tu recibo no aparece, escribe a Registro y Control Académico.',
    ],
  },
  {
    nombre: 'calendario_academico',
    categoria: 'académico',
    descripcion: 'Inicio y fin de clases, vacaciones y fechas del semestre.',
    dependencia: 'academica',
    claves: { calendario: 3, inicio: 1.5, clases: 1.5, empiezan: 1.5, inician: 1.5, vacaciones: 2.5, semestre: 1, termina: 1.5, finaliza: 1.5, parciales: 2, examenes: 2 },
    ejemplos: ['¿Dónde veo el calendario académico?', '¿Cuándo empiezan las clases?', '¿Cuándo son las vacaciones?'],
    respuestas: [
      'El calendario académico vigente (inicio y fin de clases, evaluaciones, vacaciones y fechas límite) se publica por resolución del Consejo Académico en el portal institucional: https://www.uptc.edu.co. Te sugiero revisarlo al inicio de cada semestre, porque las fechas cambian.',
    ],
  },
  {
    nombre: 'correo_institucional_activar',
    categoria: 'técnico',
    descripcion: 'Activación del correo institucional @uptc.edu.co.',
    dependencia: 'tecnologia',
    claves: { correo: 2, institucional: 2, email: 2, activar: 2, activo: 1.5, crear: 1, cuenta: 1.5, gmail: 1 },
    ejemplos: ['¿Cómo activo mi correo institucional?', 'No tengo correo de la universidad', 'Crear cuenta de email UPTC'],
    respuestas: [
      'Tu correo institucional (@uptc.edu.co) se asigna después de la matrícula. Para activarlo sigue las instrucciones que publica la DTIC en su línea de atención: https://www.uptc.edu.co/sitio/portal/sitios/universidad/rectoria/dtics/09_linea_aten.html. Si pasaron varios días y no lo tienes, repórtalo allí mismo.',
    ],
  },
  {
    nombre: 'correo_recuperar_contrasena',
    categoria: 'técnico',
    descripcion: 'Recuperación de contraseña del correo o de las plataformas.',
    dependencia: 'tecnologia',
    claves: { contrasena: 3, clave: 2.5, olvide: 2.5, recuperar: 2.5, restablecer: 2.5, bloqueado: 2, bloqueada: 2, password: 3 },
    ejemplos: ['Olvidé la contraseña del correo', 'Mi cuenta está bloqueada', 'Recuperar clave'],
    respuestas: [
      'Para recuperar tu contraseña usa la opción «¿Olvidaste tu contraseña?» de la plataforma y sigue los pasos con tu correo de recuperación. Si la cuenta sigue bloqueada, solicita el restablecimiento a la Línea de Atención de la DTIC: https://www.uptc.edu.co/sitio/portal/sitios/universidad/rectoria/dtics/09_linea_aten.html',
    ],
  },
  {
    nombre: 'carnet_estudiantil',
    categoria: 'administrativo',
    descripcion: 'Solicitud o duplicado del carné estudiantil.',
    dependencia: 'registro',
    claves: { carnet: 3, carne: 3, plastico: 2.5, tarjeta: 1.5, identificacion: 1.5, estudiantil: 1, duplicado: 1.5, saco: 1, sacar: 1 },
    ejemplos: ['¿Cómo saco el carnet?', 'Necesito el plástico ese de estudiante', 'Perdí el carné, ¿cómo pido un duplicado?'],
    respuestas: [
      'El carné estudiantil se tramita después de estar matriculado. Revisa en el portal institucional las indicaciones vigentes para la foto y la entrega. Si lo perdiste, puedes solicitar un duplicado ante Registro y Control Académico.',
    ],
  },
  {
    nombre: 'certificado_estudio',
    categoria: 'administrativo',
    descripcion: 'Certificados y constancias de estudio o de notas.',
    dependencia: 'registro',
    claves: { certificado: 3, certificados: 3, constancia: 3, notas: 1.5, calificaciones: 1.5, estudio: 1, pido: 1, solicitar: 1 },
    ejemplos: ['¿Cómo pido un certificado de estudio?', 'Necesito una constancia de que estudio', 'Certificado de notas'],
    respuestas: [
      'Los certificados y constancias (de estudio o de calificaciones) se solicitan ante Registro y Control Académico. Por lo general debes hacer la solicitud, pagar el derecho correspondiente y esperar el tiempo de expedición indicado en el portal.',
    ],
  },
  {
    nombre: 'cancelacion_asignaturas',
    categoria: 'académico',
    descripcion: 'Cancelación de asignaturas o del semestre.',
    dependencia: 'registro',
    claves: { cancelar: 3, cancelacion: 3, retirar: 2, retiro: 2, materia: 1.5, materias: 1.5, asignatura: 1.5, asignaturas: 1.5 },
    ejemplos: ['¿Cómo cancelo una materia?', 'Quiero retirar una asignatura', 'Cancelación de semestre'],
    respuestas: [
      'La cancelación de asignaturas solo se puede hacer dentro del plazo que fija el calendario académico y según el Reglamento Estudiantil. Revisa la fecha límite en el calendario vigente y haz la solicitud en la plataforma académica o ante tu Escuela.',
    ],
  },
  {
    nombre: 'reintegro_aplazamiento',
    categoria: 'académico',
    descripcion: 'Reintegro, aplazamiento o reserva de cupo.',
    dependencia: 'registro',
    claves: { reintegro: 3, reintegrarme: 3, aplazar: 3, aplazamiento: 3, reserva: 2, cupo: 2, volver: 1.5, retomar: 2 },
    ejemplos: ['¿Cómo solicito reintegro?', 'Quiero aplazar el semestre', 'Reserva de cupo'],
    respuestas: [
      'Las solicitudes de reintegro y de aplazamiento se tramitan ante Registro y Control Académico dentro de las fechas del calendario académico, según las condiciones del Reglamento Estudiantil.',
    ],
  },
  {
    nombre: 'horarios_clases',
    categoria: 'académico',
    descripcion: 'Consulta de horarios de clase y salones.',
    dependencia: 'academica',
    claves: { horario: 3, horarios: 3, salon: 2.5, salones: 2.5, clase: 1.5, aula: 1.5, grupo: 1, docente: 1 },
    ejemplos: ['¿Dónde veo mi horario?', '¿En qué salón tengo clase?', 'Horario de clases'],
    respuestas: [
      'Tu horario y los salones aparecen en la plataforma académica una vez inscribes las asignaturas. Si un grupo no muestra salón, consulta con la Escuela de tu programa.',
    ],
  },
  {
    nombre: 'bienestar_universitario',
    categoria: 'administrativo',
    descripcion: 'Servicios de Bienestar Universitario (salud, psicología, deporte, apoyos).',
    dependencia: 'bienestar',
    claves: { bienestar: 3, psicologia: 2.5, psicologo: 2.5, salud: 2, medico: 2, deporte: 2, deportes: 2, restaurante: 2, comedor: 2, subsidio: 2, apoyo: 1, beca: 2, becas: 2 },
    ejemplos: ['¿Hay servicio de psicología?', 'Información del restaurante estudiantil', '¿Qué apoyos da Bienestar?'],
    respuestas: [
      'Bienestar Universitario ofrece servicios de salud, acompañamiento psicológico, deporte, cultura y programas de apoyo socioeconómico. Consulta las convocatorias y horarios vigentes en el portal o directamente en la oficina de Bienestar Universitario de tu sede.',
    ],
  },
  {
    nombre: 'biblioteca',
    categoria: 'académico',
    descripcion: 'Servicios de biblioteca y bases de datos.',
    dependencia: 'academica',
    claves: { biblioteca: 4, libro: 2, libros: 2, prestamo: 2, bases: 2, datos: 1, revistas: 1.5, horario: 0.8 },
    ejemplos: ['¿Cuál es el horario de la biblioteca?', '¿Cómo pido un libro prestado?', 'Acceso a bases de datos'],
    respuestas: [
      'La biblioteca presta servicios de préstamo, consulta en sala y acceso a bases de datos académicas con tu cuenta institucional. Revisa horarios y servicios en el portal de la UPTC.',
    ],
  },
  {
    nombre: 'acceso_plataformas',
    categoria: 'técnico',
    descripcion: 'Problemas para ingresar a las plataformas académicas o virtuales.',
    dependencia: 'tecnologia',
    claves: { plataforma: 2.5, plataformas: 2.5, ingresar: 1, entrar: 1, aula: 1, virtual: 2, error: 1.5, sirve: 1, funciona: 1.5, carga: 1 },
    ejemplos: ['No puedo entrar a la plataforma', 'El aula virtual no me funciona', 'Me sale error al ingresar'],
    respuestas: [
      'Si no puedes ingresar a una plataforma: 1) verifica que uses tu usuario institucional, 2) prueba en otro navegador o en una ventana privada, 3) intenta recuperar la contraseña. Si el error continúa, repórtalo con una captura a la Línea de Atención de la DTIC: https://www.uptc.edu.co/sitio/portal/sitios/universidad/rectoria/dtics/09_linea_aten.html',
    ],
  },
  {
    nombre: 'orientacion_primer_semestre',
    categoria: 'académico',
    descripcion: 'Orientación general para estudiantes nuevos.',
    dependencia: 'academica',
    claves: { nuevo: 2, nueva: 2, primer: 2, primiparo: 3, induccion: 3, empezar: 1.5, perdido: 2, orientacion: 2.5 },
    ejemplos: ['Soy nuevo, ¿por dónde empiezo?', 'Información para primer semestre', '¿Cuándo es la inducción?'],
    respuestas: [
      '¡Bienvenido a la UPTC! Para empezar: 1) completa tu matrícula financiera y académica, 2) activa tu correo institucional, 3) consulta tu horario en la plataforma académica y 4) revisa el calendario académico. Atento también a la jornada de inducción que programa la universidad. Pregúntame por cualquiera de estos pasos.',
    ],
  },
];

// Consultas fuera de dominio para generar historial de ejemplo (terminan escaladas).
export const CONSULTAS_FUERA_DOMINIO = [
  '¿Quién ganó el partido de ayer?',
  'Necesito hablar con mi profesor de cálculo',
  '¿Me pueden cambiar la nota del parcial?',
  'Tengo un problema con mi pago de hace dos semestres',
  '¿Dónde queda la oficina del decano?',
  'Quiero poner una queja',
  'Homologación de materias de otra universidad',
  '¿Puedo hacer doble programa?',
];

export const USUARIOS_SEED = [
  {
    id: 1,
    nombre: 'Administrador General',
    correo: 'admin@uptc.edu.co',
    password: 'Admin2026*',
    rol: 'superadministrador',
    activo: true,
    ultimoAcceso: null,
  },
  {
    id: 2,
    nombre: 'Gestor de Conocimiento',
    correo: 'gestor@uptc.edu.co',
    password: 'Gestor2026*',
    rol: 'administrador',
    activo: true,
    ultimoAcceso: null,
  },
];

export const CONFIG_DEFECTO = {
  umbralConfianza: 0.6,
  inactividadSesionMin: 30,
  plantillaEscalamiento:
    'No tengo información suficiente para responder esa pregunta con seguridad. Te recomiendo comunicarte con {dependencia}.',
  notificarPorCorreo: true,
  dependencias: DEPENDENCIAS,
  // Solo en modo demostración:
  simularFalloPln: false,
};
