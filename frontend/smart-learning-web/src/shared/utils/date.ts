export const formatDate = (value: string | null | undefined, fallback = '--'): string => {
  if (!value) {
    return fallback;
  }

  return new Intl.DateTimeFormat(
    'vi-VN',
    {
      dateStyle: 'medium',
    },
  ).format(new Date(value));
}

export const formatDateTime = (value: string | null | undefined, fallback = '--'): string => {
  if (!value) {
    return fallback;
  }

  return new Intl.DateTimeFormat(
    'vi-VN',
    {
      dateStyle: 'medium',
      timeStyle: 'short',
    },
  ).format(new Date(value));
}