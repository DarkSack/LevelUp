export const metadata = { title: 'LevelUp Backend', description: 'GraphQL API for LevelUp' };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body style={{ margin: 0 }}>{children}</body>
    </html>
  );
}
