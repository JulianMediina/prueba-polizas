// Genera el documento único de entrega (PDF) a partir de los Markdown de docs/.
// Uso: cd docs/entrega && npm ci && npm run pdf
// Requiere Google Chrome o Chromium instalado (CHROME_PATH para indicar otra ruta).
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { Marked } from 'marked';
import puppeteer from 'puppeteer-core';

const AQUI = path.dirname(fileURLToPath(import.meta.url));
const RAIZ = path.resolve(AQUI, '..', '..');
const REPO = 'https://github.com/JulianMediina/prueba-polizas';
const SALIDA = path.join(AQUI, 'Prueba-Desarrollador-TI-Julian-Medina.pdf');

const CANDIDATO = 'Julián Medina';
const SECCIONES = [
  'docs/modulo-1-arquitectura.md',
  'docs/modulo-2-codigo.md',
  'docs/modulo-3-sql.md',
  'docs/modulo-4-git.md',
  'docs/modulo-5-liderazgo.md',
];
const ANEXOS = [
  'docs/adr/0001-microservicios-con-api-gateway.md',
  'docs/adr/0002-event-driven-con-outbox.md',
  'docs/adr/0003-acl-hexagonal-para-core.md',
  'docs/adr/0004-despliegue-demo-ec2.md',
];

const CHROME_CANDIDATOS = [
  process.env.CHROME_PATH,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
  '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
].filter(Boolean);

const slug = (texto) =>
  texto
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');

/** Convierte un .md en HTML: Mermaid como <pre class="mermaid">, enlaces relativos → GitHub. */
function renderizar(archivo, { nivelBase = 1, prefijoId }) {
  const directorio = path.posix.dirname(archivo);
  const titulos = [];
  const marked = new Marked({
    gfm: true,
    renderer: {
      code({ text, lang }) {
        const seguro = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        if (lang === 'mermaid') return `<pre class="mermaid">${seguro}</pre>`;
        return `<pre class="codigo"><code>${seguro}</code></pre>`;
      },
      heading({ tokens, depth }) {
        const texto = this.parser.parseInline(tokens);
        const nivel = Math.min(depth + nivelBase - 1, 6);
        const id = `${prefijoId}-${slug(texto.replace(/<[^>]+>/g, ''))}`;
        titulos.push({ nivel, texto: texto.replace(/<[^>]+>/g, ''), id });
        return `<h${nivel} id="${id}">${texto}</h${nivel}>`;
      },
      link({ href, title, tokens }) {
        const texto = this.parser.parseInline(tokens);
        let destino = href;
        if (!/^(https?:|mailto:|#)/.test(href)) {
          const relativo = path.posix.normalize(path.posix.join(directorio, href));
          const tipo = /\.[a-z0-9]+$/i.test(relativo) ? 'blob' : 'tree';
          destino = `${REPO}/${tipo}/main/${relativo}`;
        }
        return `<a href="${destino}"${title ? ` title="${title}"` : ''}>${texto}</a>`;
      },
    },
  });
  const html = marked.parse(readFileSync(path.join(RAIZ, archivo), 'utf8'));
  return { html, titulos };
}

function construirHtml() {
  const partes = [];
  const indice = [];

  SECCIONES.forEach((archivo, i) => {
    const { html, titulos } = renderizar(archivo, { prefijoId: `m${i + 1}` });
    partes.push(`<section class="modulo">${html}</section>`);
    indice.push(...titulos.filter((t) => t.nivel <= 2));
  });

  const anexos = ANEXOS.map((archivo, i) => renderizar(archivo, { nivelBase: 2, prefijoId: `adr${i + 1}` }));
  partes.push(
    `<section class="modulo"><h1 id="anexo-adr">Anexo – Registros de decisiones de arquitectura (ADR)</h1>
     <p>Cada ADR documenta una decisión del Módulo 1 o del despliegue: contexto, decisión y consecuencias.</p>
     ${anexos.map((a) => `<div class="adr">${a.html}</div>`).join('\n')}</section>`,
  );
  indice.push({ nivel: 1, texto: 'Anexo – Registros de decisiones de arquitectura (ADR)', id: 'anexo-adr' });
  anexos.forEach((a) => indice.push(...a.titulos.filter((t) => t.nivel === 2)));

  const fecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' }).format(new Date());
  const portada = `
    <section class="portada">
      <div class="marca"></div>
      <p class="etiqueta">Prueba de conocimientos</p>
      <h1 class="titulo">Desarrollador TI / Sénior</h1>
      <p class="subtitulo">Plataforma de Gestión de Pólizas de Arrendamiento</p>
      <dl>
        <dt>Candidato</dt><dd>${CANDIDATO}</dd>
        <dt>Fecha</dt><dd>${fecha}</dd>
        <dt>Repositorio (Módulo 2)</dt><dd><a href="${REPO}">${REPO.replace('https://', '')}</a></dd>
        <dt>Demo en línea</dt><dd><a href="https://d3l78drj5ly5ij.cloudfront.net">d3l78drj5ly5ij.cloudfront.net</a></dd>
      </dl>
    </section>`;

  const toc = `
    <section class="indice">
      <h1 id="contenido">Contenido</h1>
      <ol>${indice
        .map((t) => `<li class="n${t.nivel}"><a href="#${t.id}">${t.texto}</a></li>`)
        .join('')}</ol>
    </section>`;

  const mermaidJs = readFileSync(createRequire(import.meta.url).resolve('mermaid/dist/mermaid.min.js'), 'utf8');
  const css = readFileSync(path.join(AQUI, 'estilos.css'), 'utf8');

  return `<!doctype html>
<html lang="es-CO"><head><meta charset="utf-8"><title>Prueba Desarrollador TI – ${CANDIDATO}</title>
<style>${css}</style></head>
<body>${portada}${toc}${partes.join('\n')}
<script>${mermaidJs}</script>
<script>
  mermaid.initialize({ startOnLoad: false, theme: 'base', securityLevel: 'strict',
    themeVariables: { primaryColor: '#e8f1f8', primaryBorderColor: '#0b3c5d', primaryTextColor: '#1f2933',
      lineColor: '#52606d', secondaryColor: '#e3f2f1', tertiaryColor: '#fdf6ea', clusterBkg: '#f7f9fb',
      clusterBorder: '#9fb3c8', actorBkg: '#e8f1f8', actorBorder: '#0b3c5d', noteBkgColor: '#fdf6ea',
      git0: '#0b3c5d', git1: '#1d7874', git2: '#f2a541', git3: '#6d94ba', git4: '#c62828',
      gitBranchLabel0: '#ffffff', gitBranchLabel1: '#ffffff', gitBranchLabel2: '#1f2933', gitBranchLabel3: '#ffffff', gitBranchLabel4: '#ffffff' },
    fontFamily: 'Segoe UI, Arial, sans-serif', flowchart: { useMaxWidth: true }, er: { useMaxWidth: true },
    sequence: { useMaxWidth: true }, gitGraph: { useMaxWidth: true } });
  window.listo = mermaid.run({ querySelector: 'pre.mermaid' }).then(() => true);
</script>
</body></html>`;
}

async function main() {
  const chrome = CHROME_CANDIDATOS.find((ruta) => existsSync(ruta));
  if (!chrome) throw new Error('No se encontró Chrome/Chromium; defina CHROME_PATH');

  const html = construirHtml();
  const htmlPath = path.join(AQUI, 'entrega.html');
  writeFileSync(htmlPath, html);

  const navegador = await puppeteer.launch({ executablePath: chrome, headless: true });
  try {
    const pagina = await navegador.newPage();
    await pagina.goto(`file:///${htmlPath.replace(/\\/g, '/')}`, { waitUntil: 'load' });
    await pagina.evaluate(() => window.listo);
    const errores = await pagina.$$eval('pre.mermaid', (els) =>
      els.filter((e) => !e.querySelector('svg') || e.textContent.includes('Syntax error')).length,
    );
    if (errores > 0) throw new Error(`${errores} diagrama(s) Mermaid no se pudieron renderizar`);
    const diagramas = await pagina.$$eval('pre.mermaid svg', (els) => els.length);

    if (process.argv.includes('--capturas')) {
      await pagina.setViewport({ width: 900, height: 1200 });
      const els = await pagina.$$('pre.mermaid');
      for (const [i, el] of els.entries()) await el.screenshot({ path: path.join(AQUI, `diagrama-${i + 1}.png`) });
    }

    await pagina.pdf({
      path: SALIDA,
      format: 'A4',
      printBackground: true,
      margin: { top: '20mm', bottom: '18mm', left: '16mm', right: '16mm' },
      displayHeaderFooter: true,
      headerTemplate: `<div style="font-size:8px;color:#7b8794;width:100%;padding:0 16mm;font-family:Segoe UI,Arial">
        Prueba Desarrollador TI / Sénior · ${CANDIDATO}</div>`,
      footerTemplate: `<div style="font-size:8px;color:#7b8794;width:100%;padding:0 16mm;text-align:right;font-family:Segoe UI,Arial">
        <span class="pageNumber"></span> / <span class="totalPages"></span></div>`,
    });
    console.log(`PDF generado: ${SALIDA} (${diagramas} diagramas)`);
  } finally {
    await navegador.close();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
