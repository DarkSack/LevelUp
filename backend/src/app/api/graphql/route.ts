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

/**
 * Yoga espera su propio contexto como segundo argumento y Next pasa el suyo
 * (`{ params }`). Daba igual en la práctica —Yoga no lo usa aquí, el contexto
 * lo construye `buildContext`— pero Next 16 comprueba la firma de los route
 * handlers al compilar y las dos no encajan. Este envoltorio deja la firma que
 * Next espera y llama a Yoga con un contexto vacío.
 */
const handler = (request: Request) => handleRequest(request, {});

export const GET = handler;
export const POST = handler;
export const OPTIONS = handler;
