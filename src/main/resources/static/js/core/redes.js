import { el, icone } from './dom.js';

// Domínio → ícone da marca (arquivos locais em /img/redes, ver LEIA-ME.txt). Qualquer outro site
// aparece com o ícone genérico de globo — os usuários podem adicionar o link que quiserem.
const REDES = [
    { dominios: ['linkedin.com'], nome: 'LinkedIn', arquivo: 'linkedin' },
    { dominios: ['github.com'], nome: 'GitHub', arquivo: 'github' },
    { dominios: ['gitlab.com'], nome: 'GitLab', arquivo: 'gitlab' },
    { dominios: ['behance.net'], nome: 'Behance', arquivo: 'behance' },
    { dominios: ['dribbble.com'], nome: 'Dribbble', arquivo: 'dribbble' },
    { dominios: ['figma.com'], nome: 'Figma', arquivo: 'figma' },
    { dominios: ['x.com', 'twitter.com'], nome: 'X', arquivo: 'x' },
    { dominios: ['instagram.com'], nome: 'Instagram', arquivo: 'instagram' },
    { dominios: ['youtube.com', 'youtu.be'], nome: 'YouTube', arquivo: 'youtube' },
    { dominios: ['facebook.com'], nome: 'Facebook', arquivo: 'facebook' },
    { dominios: ['medium.com'], nome: 'Medium', arquivo: 'medium' },
    { dominios: ['dev.to'], nome: 'DEV', arquivo: 'devdotto' },
    { dominios: ['stackoverflow.com'], nome: 'Stack Overflow', arquivo: 'stackoverflow' },
    { dominios: ['kaggle.com'], nome: 'Kaggle', arquivo: 'kaggle' },
];

/** @returns {{host: string, nome: string, arquivo: string|null} | null} null se a URL não for http(s). */
export function identificarRede(url) {
    let endereco;
    try {
        endereco = new URL(url);
    } catch {
        return null;
    }
    if (endereco.protocol !== 'https:' && endereco.protocol !== 'http:') return null;
    const host = endereco.hostname.replace(/^www\./, '');
    const rede = REDES.find((item) => item.dominios.some((dominio) => host === dominio || host.endsWith('.' + dominio)));
    return { host, nome: rede ? rede.nome : host, arquivo: rede ? rede.arquivo : null };
}

export function iconeRede(rede) {
    return rede.arquivo
        ? el('img', { class: 'tb-rede-icone', src: `/img/redes/${rede.arquivo}.svg`, alt: '', width: 16, height: 16 })
        : icone('globe');
}

/**
 * Link externo publicado por um usuário. Abre em outra aba sem dar acesso a esta janela (noopener),
 * sem vazar a URL de origem (noreferrer) e marcado como conteúdo de usuário para buscadores (ugc/nofollow).
 * O href é atribuído só depois de validar que é http(s) — nunca javascript: ou data:.
 */
export function linkExterno(url, conteudo, classe) {
    const rede = identificarRede(url);
    if (!rede) return null;
    const link = el('a', { class: classe, target: '_blank', rel: 'noopener noreferrer nofollow ugc', title: `${rede.nome} — ${url}` }, conteudo(rede));
    link.href = url;
    return link;
}
