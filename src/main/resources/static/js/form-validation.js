(function () {
    function resolveFieldLabel(field) {
        var id = field.id;
        if (id) {
            var label = document.querySelector('label[for="' + id + '"]');
            if (label && label.textContent) {
                return label.textContent.trim();
            }
        }
        return field.name || 'This field';
    }

    function validationMessage(field) {
        var validity = field.validity;
        var label = resolveFieldLabel(field);

        if (validity.valueMissing) {
            return label + ' is required.';
        }
        if (validity.typeMismatch) {
            return 'Please enter a valid value for ' + label + '.';
        }
        if (validity.patternMismatch) {
            return 'Please enter a valid format for ' + label + '.';
        }
        if (validity.rangeUnderflow) {
            return label + ' is below the minimum allowed value.';
        }
        if (validity.rangeOverflow) {
            return label + ' exceeds the maximum allowed value.';
        }
        if (validity.tooShort) {
            return label + ' is too short.';
        }
        if (validity.tooLong) {
            return label + ' is too long.';
        }
        if (validity.badInput) {
            return 'Please enter a valid value for ' + label + '.';
        }
        return '';
    }

    function ensureWarningBox(form) {
        var warningBox = form.querySelector('.client-validation-warning');
        if (!warningBox) {
            warningBox = document.createElement('div');
            warningBox.className = 'client-validation-warning';
            warningBox.style.display = 'none';
            warningBox.style.marginBottom = '10px';
            warningBox.style.padding = '8px 10px';
            warningBox.style.borderLeft = '4px solid #c33';
            warningBox.style.backgroundColor = '#fee';
            warningBox.style.color = '#900';
            form.insertBefore(warningBox, form.firstChild.nextSibling || form.firstChild);
        }
        return warningBox;
    }

    function installValidation(form) {
        var warningBox = ensureWarningBox(form);
        form.setAttribute('novalidate', 'novalidate');

        form.addEventListener('submit', function (event) {
            var fields = form.querySelectorAll('input, select, textarea');
            var firstInvalidMessage = '';

            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (typeof field.checkValidity === 'function' && !field.checkValidity()) {
                    firstInvalidMessage = validationMessage(field);
                    if (!firstInvalidMessage) {
                        firstInvalidMessage = 'Please correct the invalid input.';
                    }
                    field.setCustomValidity(firstInvalidMessage);
                    if (!form.querySelector(':focus')) {
                        field.focus();
                    }
                    break;
                }
                if (typeof field.setCustomValidity === 'function') {
                    field.setCustomValidity('');
                }
            }

            if (firstInvalidMessage) {
                event.preventDefault();
                warningBox.textContent = firstInvalidMessage;
                warningBox.style.display = 'block';
                return;
            }

            warningBox.style.display = 'none';
        });

        form.addEventListener('input', function (event) {
            var target = event.target;
            if (target && typeof target.setCustomValidity === 'function') {
                target.setCustomValidity('');
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        var forms = document.querySelectorAll('form');
        for (var i = 0; i < forms.length; i++) {
            installValidation(forms[i]);
        }
    });
})();
