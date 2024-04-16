/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import React, {useEffect, useState} from 'react';
import {ActionableNotification, Button, Stack} from '@carbon/react';

import {Icon, Labeled, Modal, Switch, Tooltip, UserTypeahead} from 'components';
import {t} from 'translation';
import {getOptimizeProfile, isEmailEnabled} from 'config';
import {useDocs} from 'hooks';

import './ConfigureProcessModal.scss';

export function ConfigureProcessModal({
  initialConfig: {
    owner,
    digest: {enabled},
  },
  onClose,
  onConfirm,
}) {
  const [selectedUser, setSelectedUser] = useState(
    owner?.id ? {id: 'USER:' + owner.id, identity: {...owner, type: 'user'}} : null
  );
  const [digestEnabled, setDigestEnabled] = useState(enabled);
  const [optimizeProfile, setOptimizeProfile] = useState();
  const [emailEnabled, setEmailEnabled] = useState();
  const {generateDocsLink} = useDocs();

  const noChangesHappened =
    digestEnabled === enabled &&
    ((!selectedUser?.identity.id && !owner?.id) || selectedUser?.identity.id === owner?.id);

  useEffect(() => {
    (async () => {
      setOptimizeProfile(await getOptimizeProfile());
      setEmailEnabled(await isEmailEnabled());
    })();
  }, []);

  return (
    <Modal open onClose={onClose} className="ConfigureProcessModal" isOverflowVisible>
      <Modal.Header>{t('processes.configureProcess')}</Modal.Header>
      <Modal.Content>
        <Stack gap={4}>
          {!emailEnabled && (
            <ActionableNotification
              kind="warning"
              subtitle={t('alert.emailWarning', {
                docsLink: generateDocsLink(
                  'self-managed/optimize-deployment/configuration/system-configuration/#email'
                ),
              })}
              className="emailWarning"
            />
          )}
          <Labeled
            label={
              <div className="infoContainer">
                {t('processes.processOwner')}{' '}
                <Tooltip align="center" content={t('processes.ownerInfo')}>
                  <Icon type="info" />
                </Tooltip>
              </div>
            }
          >
            <UserTypeahead
              key={selectedUser?.id}
              users={selectedUser ? [selectedUser] : []}
              onChange={(users) => {
                const newSelection = users[users.length - 1];
                setSelectedUser(newSelection);
                if (!newSelection) {
                  setDigestEnabled(false);
                }
              }}
              excludeGroups
              optionsOnly={optimizeProfile === 'cloud'}
            />
          </Labeled>
          <Switch
            className="digestSwitch"
            disabled={!selectedUser}
            label={
              <div className="infoContainer">
                {t('processes.emailDigest')}{' '}
                <Tooltip align="center" content={t('processes.digestInfo')}>
                  <Icon type="info" />
                </Tooltip>
              </div>
            }
            checked={digestEnabled}
            onChange={({target}) => {
              if (target.checked && selectedUser) {
                setDigestEnabled(true);
              } else {
                setDigestEnabled(false);
              }
            }}
          />
        </Stack>
      </Modal.Content>
      <Modal.Footer>
        <Button kind="secondary" className="close" onClick={onClose}>
          {t('common.cancel')}
        </Button>
        <Button
          disabled={noChangesHappened}
          className="confirm"
          onClick={() => {
            const ownerId = selectedUser?.identity.id || null;
            onConfirm(
              {
                ownerId,
                processDigest: {enabled: digestEnabled},
              },
              emailEnabled,
              selectedUser?.identity.name
            );
          }}
        >
          {t('common.save')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

export default ConfigureProcessModal;
