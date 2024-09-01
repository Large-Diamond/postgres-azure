export function olderThenOneDay(date?: Date | null): boolean {
  return !date || Date.now() - date.getTime() > 24 * 60 * 60 * 1000;
}