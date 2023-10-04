/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {Icon} from '@carbon/react/icons';
import {
  TextArea as BaseTextArea,
  IconButton as BaseIconButton,
} from '@carbon/react';
import {Container, IconContainer, TextArea, IconButton} from './styled';

interface Props extends React.ComponentProps<typeof BaseTextArea> {
  Icon: Icon;
  invalid?: boolean;
  onIconClick: () => void;
  buttonLabel: string;
  tooltipPosition?: React.ComponentProps<typeof BaseIconButton>['align'];
}

const IconTextArea: React.FC<Props> = ({
  Icon,
  invalid,
  onIconClick,
  buttonLabel,
  tooltipPosition = 'top-right',
  ...props
}) => {
  return (
    <Container $isInvalid={invalid}>
      <TextArea invalid={invalid} {...props} />
      <IconContainer $isTextArea>
        <IconButton
          kind="ghost"
          size="sm"
          onClick={onIconClick}
          label={buttonLabel}
          align={tooltipPosition}
        >
          <Icon />
        </IconButton>
      </IconContainer>
    </Container>
  );
};

export {IconTextArea};