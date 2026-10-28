import { Navigate, useParams, useLocation } from 'react-router-dom';

/** 旧路径 → /apps/*，保留 path params 与 query。 */
export default function LegacyRedirect({ to }: { to: string }) {
  const params = useParams();
  const location = useLocation();
  let target = to;
  for (const [key, value] of Object.entries(params)) {
    if (value != null) {
      target = target.replace(`:${key}`, encodeURIComponent(value));
    }
  }
  return <Navigate to={`${target}${location.search}${location.hash}`} replace />;
}
