function highlightJapaneseWords()
{
    const style = document.createElement('style');

    style.textContent = `
        .japanese-word {
            border-radius: 3px;
            padding: 0 2px;
            margin: 0 1px;
            box-shadow: 0 0 2px rgba(0,0,0,0.2);
            transition: background-color 0.3s;
        }
        .japanese-word:hover {
            background-color: #FFEE58;
        }
    `;

    document.head.appendChild(style);

    function tokenizeJapanese(text)
    {
        if (window.Intl && Intl.Segmenter) {
            const segmenter = new Intl.Segmenter('ja-JP', { granularity: 'word' });
            const segments = segmenter.segment(text);
            return Array.from(segments).map(seg => seg.segment);
        } else if (window.Intl && Intl.v8BreakIterator) {
            const it = Intl.v8BreakIterator(['ja-JP'], {type: 'word'});
            it.adoptText(text);
            const words = [];
            let cur = 0, prev = 0;
            while (cur < text.length) {
                prev = cur;
                cur = it.next();
                words.push(text.substring(prev, cur));
            }
            return words;
        }
        return [text];
    }

    function isJapanese(text)
    {
        return /[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF\u3400-\u4DBF\uFF00-\uFFEF\u3000-\u303F]/.test(text);
    }

    function walkTextNodes(node, callback)
    {
        if (node.nodeType === Node.TEXT_NODE) {
            callback(node);
        } else {
            for (let child of node.childNodes) {
                walkTextNodes(child, callback);
            }
        }
    }

    walkTextNodes(document.body, (textNode) => {
        const text = textNode.nodeValue;
        if (!isJapanese(text)) return;

        const words = tokenizeJapanese(text);
        const fragment = document.createDocumentFragment();

        words.forEach(word => {
            if (isJapanese(word)) {
                const span = document.createElement('span');
                span.className = 'japanese-word';
                span.textContent = word;
                span.addEventListener('click', function(event) {
                    event.stopPropagation();
                    prompt('JISHO_LOOKUP',word)
                });
                fragment.appendChild(span);
            } else {
                fragment.appendChild(document.createTextNode(word));
            }
        });
        textNode.parentNode.replaceChild(fragment, textNode);
    });
}

function changeHighlightingColor(color)
{
    let elements = document.querySelectorAll('.japanese-word');
    elements.forEach((element) => {
        element.style.backgroundColor = color;
    });
}

highlightJapaneseWords();
