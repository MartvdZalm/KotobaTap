window.highlightCorrections = [];
window.highlightObserver = null;
window.highlightStyleId = "japanese-highlighter-styles";

function setHighlightCorrections(correctionsJson) {
	try {
		window.highlightCorrections = JSON.parse(correctionsJson);
	} catch (e) {
		console.error("Failed to parse highlight corrections:", e);
	}
}

function applyCorrections(text) {
	for (const correction of window.highlightCorrections) {
		if (correction.originalText === text) {
			const remainingText = text.replace(correction.correctedText, "");
			return [
				correction.correctedText,
				...(remainingText ? [remainingText] : []),
			];
		}
	}

	return text;
}

function reHighlightWithCorrections() {
	if (window.highlightObserver) {
		window.highlightObserver.disconnect();
	}

	const highlightSpans = document.querySelectorAll(".japanese-word");
	highlightSpans.forEach((span) => {
		const parent = span.parentNode;
		if (parent) {
			parent.replaceChild(document.createTextNode(span.textContent), span);
			parent.normalize();
		}
	});

	document.body.removeAttribute("data-japanese-highlighted");
	highlightJapaneseWords();
}

function highlightJapaneseWords() {
	document.body.setAttribute("data-japanese-highlighted", "true");

	if (!document.getElementById(window.highlightStyleId)) {
		const style = document.createElement("style");
		style.id = window.highlightStyleId;
		style.textContent = `
						.japanese-word {
								background-color: #FFF59D;
								border-radius: 4px;
								padding: 1px 3px;
								margin: 0 1px;
								box-shadow: 0 1px 3px rgba(0,0,0,0.15);
								transition: all 0.2s ease;
								cursor: pointer;
								display: inline-block;
								position: relative;
								border: 1px solid rgba(0,0,0,0.1);
						}
						
						.japanese-word:hover {
								transform: translateY(-1px);
								box-shadow: 0 2px 6px rgba(0,0,0,0.25);
								background-color: #FFEB3B;
								border-color: rgba(0,0,0,0.2);
						}
						
						.japanese-word:active {
								transform: translateY(0);
								box-shadow: 0 1px 2px rgba(0,0,0,0.2);
						}
						
						.japanese-word.clicked {
								background-color: #FF9800;
								color: white;
								animation: clickPulse 0.3s ease;
						}
						
						@keyframes clickPulse {
								0% { transform: scale(1); }
								50% { transform: scale(1.05); }
								100% { transform: scale(1); }
						}
						
						.japanese-word.kanji {
								font-weight: 500;
								background-color: #E1F5FE;
						}
						
						.japanese-word.hiragana {
								background-color: #F3E5F5;
						}
						
						.japanese-word.katakana {
								background-color: #E8F5E8;
						}
						
						.japanese-word.corrected {
								outline: 2px dashed #4CAF50;
								outline-offset: 1px;
						}
				`;
		document.head.appendChild(style);
	}

	window.changeHighlightingColor = function (color) {
		const elements = document.querySelectorAll(".japanese-word");
		elements.forEach((element) => {
			if (!element.classList.contains("corrected")) {
				element.style.backgroundColor = color;
			}
		});

		const style = document.getElementById(window.highlightStyleId);
		if (style) {
			style.textContent = style.textContent.replace(
				/background-color:.*?([;}])/g,
				`background-color: ${color}$1`
			);
		}
	};

	window.changeFontSize = function (size) {
		document.body.style.fontSize = size + "px";
	};

	window.changeLineSpacing = function (spacing) {
		document.body.style.lineHeight = spacing;
	};

	window.toggleHighlighting = function (enable) {
		if (enable) {
			highlightJapaneseWords();
		} else {
			cleanupJapaneseHighlighting();
		}
	};

	if (!window.highlightObserver) {
		window.highlightObserver = new MutationObserver((mutations) => {
			mutations.forEach((mutation) => {
				mutation.addedNodes.forEach((node) => {
					if (node.nodeType === Node.ELEMENT_NODE) {
						processTextNodes(node);
					}
				});
			});
		});
	}

	processInChunks();
	window.highlightObserver.observe(document.body, {
		childList: true,
		subtree: true,
	});

	window.cleanupJapaneseHighlighting = function () {
		if (window.highlightObserver) {
			window.highlightObserver.disconnect();
		}
		document.body.removeAttribute("data-japanese-highlighted");
		const style = document.getElementById(window.highlightStyleId);
		if (style) {
			style.remove();
		}

		document.querySelectorAll(".japanese-word").forEach((el) => {
			el.replaceWith(el.textContent);
		});

		delete window.changeHighlightingColor;
		delete window.changeFontSize;
		delete window.changeLineSpacing;
	};
}

function isJapanese(text) {
	if (!/[一-龯ぁ-んァ-ン]/.test(text)) {
		return false;
	}
	return /[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF\u3400-\u4DBF\uFF00-\uFFEF\u3000-\u303F]/.test(
		text
	);
}

function getJapaneseType(text) {
	if (/[\u4E00-\u9FAF\u3400-\u4DBF]/.test(text)) {
		return "kanji";
	} else if (/[\u3040-\u309F]/.test(text)) {
		return "hiragana";
	} else if (/[\u30A0-\u30FF]/.test(text)) {
		return "katakana";
	}
	return "mixed";
}

function isLikelyWord(text) {
	if (
		window.highlightCorrections.some(
			(c) => c.originalText === text || c.correctedText === text
		)
	) {
		return true;
	}

	if (text.length === 1) {
		const particles = [
			"は",
			"が",
			"を",
			"に",
			"で",
			"と",
			"の",
			"へ",
			"や",
			"か",
			"も",
			"ね",
			"よ",
			"な",
		];
		const punctuation = [
			"。",
			"、",
			"！",
			"？",
			"：",
			"；",
			"（",
			"）",
			"「",
			"」",
			"『",
			"』",
		];
		return !particles.includes(text) && !punctuation.includes(text);
	}
	return true;
}

function tokenizeJapaneseWithCorrections(text) {
	const originalTokens = tokenizeJapanese(text);
	const correctedTokens = [];

	for (const token of originalTokens) {
		if (isJapanese(token)) {
			const corrected = applyCorrections(token);
			if (Array.isArray(corrected)) {
				correctedTokens.push(...corrected.filter((part) => part.length > 0));
			} else {
				correctedTokens.push(token);
			}
		} else {
			correctedTokens.push(token);
		}
	}

	return correctedTokens;
}

function tokenizeJapanese(text) {
	if (!isJapanese(text)) {
		return [text];
	}

	try {
		if (window.Intl?.Segmenter) {
			const segmenter = new Intl.Segmenter("ja-JP", { granularity: "word" });
			return Array.from(segmenter.segment(text)).map((seg) => seg.segment);
		}

		return (
			text.match(
				/[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF\u3400-\u4DBF\uFF00-\uFFEF\u3000-\u303F]+|[^\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF\u3400-\u4DBF\uFF00-\uFFEF\u3000-\u303F]+/g
			) || [text]
		);
	} catch (e) {
		console.error("Japanese tokenization failed:", e);
		return [text];
	}
}

function processTextNodes(root) {
	const treeWalker = document.createTreeWalker(
		root,
		NodeFilter.SHOW_TEXT,
		{
			acceptNode: function (node) {
				if (node.parentNode.nodeName.match(/SCRIPT|STYLE|TEXTAREA|CODE|PRE/i)) {
					return NodeFilter.FILTER_SKIP;
				}
				return isJapanese(node.nodeValue)
					? NodeFilter.FILTER_ACCEPT
					: NodeFilter.FILTER_SKIP;
			},
		},
		false
	);

	const textNodes = [];
	while (treeWalker.nextNode()) {
		textNodes.push(treeWalker.currentNode);
	}

	for (let i = textNodes.length - 1; i >= 0; i--) {
		const textNode = textNodes[i];
		const words = tokenizeJapaneseWithCorrections(textNode.nodeValue);

		if (words.length <= 1) {
			continue;
		}

		const fragment = document.createDocumentFragment();
		words.forEach((word) => {
			if (isJapanese(word) && isLikelyWord(word)) {
				const span = document.createElement("span");
				const japaneseType = getJapaneseType(word);
				span.className = `japanese-word ${japaneseType}`;

				const isCorrected = window.highlightCorrections.some(
					(c) => c.correctedText === word
				);
				if (isCorrected) {
					span.classList.add("corrected");
				}

				span.textContent = word;
				span.title = `Click to look up: ${word}`;

				span.addEventListener("click", function (event) {
					event.stopPropagation();
					event.preventDefault();

					span.classList.add("clicked");
					setTimeout(() => span.classList.remove("clicked"), 300);

					const wordData = {
						word: word,
						type: japaneseType,
						context: getWordContext(span),
					};

					prompt("JISHO_LOOKUP", JSON.stringify(wordData));
				});

				span.addEventListener("mouseenter", function () {
					span.style.zIndex = "1000";
				});

				span.addEventListener("mouseleave", function () {
					span.style.zIndex = "auto";
				});

				fragment.appendChild(span);
			} else {
				fragment.appendChild(document.createTextNode(word));
			}
		});

		textNode.parentNode.replaceChild(fragment, textNode);
	}
}

function processInChunks() {
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

function getWordContext(element) {
	const parent = element.parentNode;
	if (!parent) return "";

	const siblings = Array.from(parent.childNodes);
	const index = siblings.indexOf(element);

	let context = "";
	for (
		let i = Math.max(0, index - 2);
		i <= Math.min(siblings.length - 1, index + 2);
		i++
	) {
		if (siblings[i].nodeType === Node.TEXT_NODE) {
			context += siblings[i].textContent;
		} else if (
			siblings[i].classList &&
			siblings[i].classList.contains("japanese-word")
		) {
			context += siblings[i].textContent;
		}
	}

	return context.trim();
}
