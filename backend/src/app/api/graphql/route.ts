import { createYoga } from 'graphql-yoga';
import { schema } from '@/graphql/schema';
import { buildContext } from '@/graphql/context';

const { handleRequest } = createYoga({
  schema,
  context: buildContext,
  graphqlEndpoint: '/api/graphql',
  fetchAPI: { Response },
  cors: {
    origin: '*',
    credentials: true,
    methods: ['GET', 'POST', 'OPTIONS'],
    allowedHeaders: ['content-type', 'authorization'],
  },
  landingPage: false,
});

export const GET     = handleRequest;
export const POST    = handleRequest;
export const OPTIONS = handleRequest;
