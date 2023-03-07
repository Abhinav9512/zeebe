/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {Button, Modal, TextEditor} from 'components';
import {useState} from 'react';
import {isTextReportValid} from 'services';
import {t} from 'translation';

export default function TextReportEditModal({initialValue, onClose, onConfirm}) {
  const [text, setText] = useState(initialValue);

  const textLength = TextEditor.getEditorStateLength(text);

  const onUpdate = () => {
    onConfirm(text);
    onClose();
  };

  return (
    <Modal open onClose={onClose}>
      <Modal.Header>{t('common.editName', {name: t('report.textReport')})}</Modal.Header>
      <Modal.Content>
        <TextEditor initialValue={initialValue} onChange={setText} />
        <TextEditor.CharCount editorState={text} />
      </Modal.Content>
      <Modal.Actions>
        <Button main onClick={onClose}>
          {t('common.cancel')}
        </Button>
        <Button main primary onClick={onUpdate} disabled={!isTextReportValid(textLength)}>
          {t('common.save')}
        </Button>
      </Modal.Actions>
    </Modal>
  );
}
