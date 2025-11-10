FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json pnpm-lock.yaml* package-lock.json* yarn.lock* ./
RUN if [ -f pnpm-lock.yaml ]; then npm install -g pnpm && pnpm install; \
    elif [ -f yarn.lock ]; then yarn install; \
    elif [ -f package-lock.json ]; then npm install; \
    else npm install; fi
COPY . .
RUN if [ -f pnpm-lock.yaml ]; then pnpm build; \
    elif [ -f yarn.lock ]; then yarn build; \
    else npm run build; fi

FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app/dist ./dist
RUN npm install -g serve
EXPOSE 5173
CMD ["serve", "-s", "dist", "-l", "5173"]
