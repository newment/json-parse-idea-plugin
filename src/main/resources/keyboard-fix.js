/**
 * Cross-platform keyboard shortcuts fix for JCEF
 * Supports: macOS (Cmd+Arrow), Windows/Linux (Ctrl+Arrow)
 */
(function() {
    'use strict';

    var isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
    var modKey = isMac ? 'metaKey' : 'ctrlKey';

    function isTextInput(target) {
        return target &&
               (target.tagName === 'TEXTAREA' ||
                target.tagName === 'INPUT' ||
                (target.isContentEditable && target.contentEditable === 'true'));
    }

    function moveToLineEnd(textArea) {
        var value = textArea.value;
        var pos = textArea.selectionStart;
        var lineStart = value.lastIndexOf('\n', pos - 1) + 1;
        var lineEnd = value.indexOf('\n', pos);
        if (lineEnd === -1) lineEnd = value.length;
        textArea.setSelectionRange(lineEnd, lineEnd);
        textArea.scrollTop = textArea.scrollHeight;
    }

    function moveToLineStart(textArea) {
        var value = textArea.value;
        var pos = textArea.selectionStart;
        var lineStart = value.lastIndexOf('\n', pos - 1) + 1;
        textArea.setSelectionRange(lineStart, lineStart);
    }

    function moveToTextStart(textArea) {
        textArea.setSelectionRange(0, 0);
        textArea.scrollTop = 0;
    }

    function moveToTextEnd(textArea) {
        var length = textArea.value.length;
        textArea.setSelectionRange(length, length);
        textArea.scrollTop = textArea.scrollHeight;
    }

    function moveToNextWord(textArea) {
        var value = textArea.value;
        var pos = textArea.selectionStart;
        var nextWord = value.indexOf(/\s/, pos);
        var newPos = nextWord === -1 ? value.length : nextWord;
        while (newPos < value.length && /\s/.test(value[newPos])) newPos++;
        textArea.setSelectionRange(newPos, newPos);
    }

    function moveToPrevWord(textArea) {
        var value = textArea.value;
        var pos = textArea.selectionStart;
        var prevWord = value.lastIndexOf(/\s/, pos - 1);
        var newPos = prevWord === -1 ? 0 : prevWord;
        while (newPos > 0 && /\s/.test(value[newPos - 1])) newPos--;
        textArea.setSelectionRange(newPos, newPos);
    }

    function deleteToLineStart(textArea) {
        var start = textArea.selectionStart;
        var end = textArea.selectionEnd;
        if (start !== end) {
            textArea.value = textArea.value.substring(0, start) + textArea.value.substring(end);
            textArea.setSelectionRange(start, start);
        } else {
            var lineStart = textArea.value.lastIndexOf('\n', start - 1) + 1;
            textArea.value = textArea.value.substring(0, lineStart) + textArea.value.substring(start);
            textArea.setSelectionRange(lineStart, lineStart);
        }
        textArea.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function deleteToLineEnd(textArea) {
        var start = textArea.selectionStart;
        var end = textArea.selectionEnd;
        var value = textArea.value;
        if (start !== end) {
            textArea.value = value.substring(0, start) + value.substring(end);
            textArea.setSelectionRange(start, start);
        } else {
            var nextNewline = value.indexOf('\n', end);
            var lineEnd = nextNewline === -1 ? value.length : nextNewline;
            textArea.value = value.substring(0, start) + value.substring(lineEnd);
            textArea.setSelectionRange(start, start);
        }
        textArea.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function deleteNextWord(textArea) {
        var start = textArea.selectionStart;
        var end = textArea.selectionEnd;
        var value = textArea.value;
        if (start !== end) {
            textArea.value = value.substring(0, start) + value.substring(end);
            textArea.setSelectionRange(start, start);
        } else {
            var nextWord = value.indexOf(/\s/, end);
            var nextPos = nextWord === -1 ? value.length : nextWord;
            while (nextPos < value.length && /\s/.test(value[nextPos])) nextPos++;
            textArea.value = value.substring(0, start) + value.substring(nextPos);
            textArea.setSelectionRange(start, start);
        }
        textArea.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function deletePrevWord(textArea) {
        var start = textArea.selectionStart;
        var end = textArea.selectionEnd;
        var value = textArea.value;
        if (start !== end) {
            textArea.value = value.substring(0, start) + value.substring(end);
            textArea.setSelectionRange(start, start);
        } else {
            var prevWord = value.lastIndexOf(/\s/, start - 1);
            var prevPos = prevWord === -1 ? 0 : prevWord;
            while (prevPos > 0 && /\s/.test(value[prevPos - 1])) prevPos--;
            textArea.value = value.substring(0, prevPos) + value.substring(start);
            textArea.setSelectionRange(prevPos, prevPos);
        }
        textArea.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function insertNewline(textArea) {
        var start = textArea.selectionStart;
        var end = textArea.selectionEnd;
        var value = textArea.value;
        textArea.value = value.substring(0, start) + '\n' + value.substring(end);
        textArea.setSelectionRange(start + 1, start + 1);
        textArea.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function handleShortcut(e) {
        var target = e.target;
        if (!isTextInput(target)) return;

        var key = e.key;
        var modPressed = e[modKey];

        if (!modPressed) return;

        var handled = true;

        if (isMac) {
            // macOS: Cmd+Arrow
            switch (key) {
                case 'ArrowRight':
                    e.preventDefault();
                    moveToLineEnd(target);
                    break;
                case 'ArrowLeft':
                    e.preventDefault();
                    moveToLineStart(target);
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    moveToTextStart(target);
                    break;
                case 'ArrowDown':
                    e.preventDefault();
                    moveToTextEnd(target);
                    break;
                case 'Backspace':
                    e.preventDefault();
                    deleteToLineStart(target);
                    break;
                case 'Delete':
                    e.preventDefault();
                    deleteToLineEnd(target);
                    break;
                case 'Enter':
                    e.preventDefault();
                    insertNewline(target);
                    break;
                default:
                    handled = false;
            }
        } else {
            // Windows/Linux: Ctrl+Arrow
            switch (key) {
                case 'ArrowRight':
                    e.preventDefault();
                    moveToNextWord(target);
                    break;
                case 'ArrowLeft':
                    e.preventDefault();
                    moveToPrevWord(target);
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    moveToTextStart(target);
                    break;
                case 'ArrowDown':
                    e.preventDefault();
                    moveToTextEnd(target);
                    break;
                case 'Backspace':
                    e.preventDefault();
                    deletePrevWord(target);
                    break;
                case 'Delete':
                    e.preventDefault();
                    deleteNextWord(target);
                    break;
                case 'Enter':
                    e.preventDefault();
                    insertNewline(target);
                    break;
                default:
                    handled = false;
            }
        }

        if (handled) e.preventDefault();
    }

    document.addEventListener('keydown', handleShortcut, true);
})();
