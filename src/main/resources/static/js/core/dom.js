// Construção de DOM sem innerHTML: todo texto vindo da API entra como textContent,
// então um nome de projeto como "<img onerror=...>" aparece literalmente, sem executar.

/**
 * el('a', { class: 'x', href: '/p', text: 'Oi', on: { click: fn } }, filho1, 'texto', ...)
 */
export function el(tag, props = {}, ...filhos) {
    const elemento = document.createElement(tag);
    for (const [chave, valor] of Object.entries(props)) {
        if (valor === undefined || valor === null || valor === false) continue;
        if (chave === 'class') elemento.className = valor;
        else if (chave === 'text') elemento.textContent = valor;
        else if (chave === 'on') Object.entries(valor).forEach(([evento, fn]) => elemento.addEventListener(evento, fn));
        else if (chave === 'dataset') Object.assign(elemento.dataset, valor);
        else if (chave === 'href') elemento.setAttribute('href', linkSeguro(valor));
        else if (valor === true) elemento.setAttribute(chave, '');
        else elemento.setAttribute(chave, String(valor));
    }
    anexar(elemento, filhos);
    return elemento;
}

function anexar(pai, filhos) {
    for (const filho of filhos.flat(Infinity)) {
        if (filho === null || filho === undefined || filho === false) continue;
        pai.append(filho instanceof Node ? filho : document.createTextNode(String(filho)));
    }
}

/** Só caminhos internos, data: de imagem gerada pelo servidor ou âncoras; bloqueia javascript: e afins. */
export function linkSeguro(url) {
    const texto = String(url);
    if (texto.startsWith('/') && !texto.startsWith('//')) return texto;
    if (texto.startsWith('#') || texto.startsWith('mailto:')) return texto;
    return '#';
}

export function substituir(alvo, ...filhos) {
    const elemento = typeof alvo === 'string' ? document.getElementById(alvo) : alvo;
    elemento.replaceChildren();
    anexar(elemento, filhos);
    renderizarIcones(elemento);
    return elemento;
}

export function icone(nome, rotulo) {
    const i = el('i', { 'data-lucide': nome, 'aria-hidden': rotulo ? null : 'true' });
    if (rotulo) {
        i.setAttribute('role', 'img');
        i.setAttribute('aria-label', rotulo);
    }
    return i;
}

export function renderizarIcones(raiz = document) {
    if (window.lucide) window.lucide.createIcons({ root: raiz, attrs: { 'stroke-width': 1.75 } });
}

export function carregando(texto = 'Carregando…') {
    return el('div', { class: 'tb-carregando', role: 'status', text: texto });
}

export function vazio(texto, ...acoes) {
    return el('div', { class: 'tb-vazio' }, el('p', { class: 'mb-2', text: texto }), acoes);
}

export function $(seletor, raiz = document) {
    return raiz.querySelector(seletor);
}

export function idDaUrl(posicao = 2) {
    const partes = window.location.pathname.split('/').filter(Boolean);
    const id = Number(partes[posicao - 1]);
    return Number.isInteger(id) && id > 0 ? id : null;
}
