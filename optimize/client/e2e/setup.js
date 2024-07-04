/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import fetch from 'node-fetch';

import config from './config';

export async function cleanEntities({ctx}) {
  const indicesToDelete = [
    'optimize-single-process-report',
    'optimize-report-share',
    'optimize-dashboard-share',
    'optimize-collection',
    'optimize-alert',
    'optimize-dashboard',
  ];

  deleteIndicesContent(
    indicesToDelete,
    ctx.users.map((user) => user.id)
  );
}

async function deleteIndicesContent(indicesToDelete, users) {
  try {
    for (const index of indicesToDelete) {
      const response = await fetch(`${config.elasticSearchEndpoint}/${index}/_delete_by_query`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          query: {
            bool: {
              must_not: [
                {
                  term: {
                    managementDashboard: 'true',
                  },
                },
                {
                  term: {
                    'data.managementReport': 'true',
                  },
                },
              ],
              must: [
                {
                  terms: {
                    owner: users,
                  },
                },
              ],
            },
          },
        }),
      });

      if (!response.ok) {
        console.error(`Failed to delete content of index ${index}. Status: ${response.status}`);
      }
    }
  } catch (error) {
    console.error('Error occurred:', error);
  }
}