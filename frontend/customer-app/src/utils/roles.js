/**
 * Role priority mirrors how accounts actually work in this system: admin
 * promotion (start-infra.sh's documented SQL workaround) strips the
 * CUSTOMER role off the account it's granted to, so in practice every
 * account carries exactly one meaningful role. Priority order here is
 * just a tiebreaker for the (currently theoretical) case of more than one.
 */
const PRIORITY = ['ADMIN', 'MERCHANT', 'DELIVERY_PARTNER', 'CUSTOMER'];

export function primaryRole(auth) {
  const roles = auth?.roles || [];
  return PRIORITY.find((r) => roles.includes(r)) || 'CUSTOMER';
}

export function homePathForRole(role) {
  switch (role) {
    case 'ADMIN':
      return '/admin';
    case 'MERCHANT':
      return '/merchant';
    case 'DELIVERY_PARTNER':
      return '/delivery';
    default:
      return '/';
  }
}

export function homePath(auth) {
  return homePathForRole(primaryRole(auth));
}
