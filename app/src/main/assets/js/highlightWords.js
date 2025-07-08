function highlightJapaneseWords()
{
    if (document.body.hasAttribute('data-japanese-highlighted')) {
        return;
    }

    document.body.setAttribute('data-japanese-highlighted', 'true');

    const styleId = 'japanese-highlighter-styles';

    if (!document.getElementById(styleId)) {
        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            .japanese-word {
                background-color: #FFF59D;
                border-radius: 3px;
                padding: 0 2px;
                margin: 0 1px;
                box-shadow: 0 0 2px rgba(0,0,0,0.2);
                transition: background-color 0.3s;
                cursor: pointer;
                display: inline-block;
            }
        `;
        document.head.appendChild(style);
    }

    window.changeHighlightingColor = function(color) {
        const elements = document.querySelectorAll('.japanese-word');
        elements.forEach((element) => {
            element.style.backgroundColor = color;
        });
        
        const style = document.getElementById(styleId);
        if (style) {
            style.textContent = style.textContent.replace(
                /background-color:.*?([;}])/g, 
                `background-color: ${color}$1`
            );
        }
    };

    function isJapanese(text)
    {
        if (!/[一-龯ぁ-んァ-ン]/.test(text)) {
            return false;
        }
        return /[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF\u3400-\u4DBF\uFF00-\uFFEF\u3000-\u303F]/.test(text);
    }

    function tokenizeJapanese(text)
    {
        if (!isJapanese(text)) {
            return [text];
        }

        try {
            if (window.Intl?.Segmenter) {
                const segmenter = new Intl.Segmenter('ja-JP', { granularity: 'word' });
                return Array.from(segmenter.segment(text)).map(seg => seg.segment);
            }
            
            return text.match(/[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF\u3400-\u4DBF\uFF00-\uFFEF\u3000-\u303F]+|[^\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF\u3400-\u4DBF\uFF00-\uFFEF\u3000-\u303F]+/g) || [text];
        } catch (e) {
            console.error('Japanese tokenization failed:', e);
            return [text];
        }
    }

    function processTextNodes(root)
    {
        const treeWalker = document.createTreeWalker(
            root,
            NodeFilter.SHOW_TEXT,
            { 
                acceptNode: function(node) {
                    if (node.parentNode.nodeName.match(/SCRIPT|STYLE|TEXTAREA|CODE|PRE/i)) {
                        return NodeFilter.FILTER_SKIP;
                    }
                    return isJapanese(node.nodeValue) ? 
                        NodeFilter.FILTER_ACCEPT : 
                        NodeFilter.FILTER_SKIP;
                }
            },
            false
        );

        const textNodes = [];
        while (treeWalker.nextNode()) {
            textNodes.push(treeWalker.currentNode);
        }

        for (let i = textNodes.length - 1; i >= 0; i--) {
            const textNode = textNodes[i];
            const words = tokenizeJapanese(textNode.nodeValue);
            
            if (words.length <= 1) {
                continue;
            }

            const fragment = document.createDocumentFragment();
            words.forEach(word => {
                if (isJapanese(word)) {
                    const span = document.createElement('span');
                    span.className = 'japanese-word';
                    span.textContent = word;
                    span.addEventListener('click', function(event) {
                        event.stopPropagation();
                        prompt('JISHO_LOOKUP', word);
                    });
                    fragment.appendChild(span);
                } else {
                    fragment.appendChild(document.createTextNode(word));
                }
            });
            
            textNode.parentNode.replaceChild(fragment, textNode);
        }
    }

    function processInChunks()
    {
        const chunks = [document.body];
        let index = 0;
        
        function processNextChunk() {
            if (index >= chunks.length) {
                return;
            }
            
            const chunk = chunks[index++];
            processTextNodes(chunk);
            
            if (chunk.children && chunk.children.length < 100) {
                chunks.push(...chunk.children);
            }
            
            if (index < chunks.length) {
                setTimeout(processNextChunk, 0);
            }
        }
        
        processNextChunk();
    }

    processInChunks();

    const observer = new MutationObserver(mutations => {
        mutations.forEach(mutation => {
            mutation.addedNodes.forEach(node => {
                if (node.nodeType === Node.ELEMENT_NODE) {
                    processTextNodes(node);
                }
            });
        });
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });

    window.cleanupJapaneseHighlighting = function() {
        observer.disconnect();
        document.body.removeAttribute('data-japanese-highlighted');
        const style = document.getElementById(styleId);
        if (style) {
            style.remove();
        }
        
        document.querySelectorAll('.japanese-word').forEach(el => {
            el.replaceWith(el.textContent);
        });

        delete window.changeHighlightingColor;
    };
}

highlightJapaneseWords();
