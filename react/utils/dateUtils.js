export const formatDate = (date) => {
  // Format javascript date as string like "June 23, 2023"
  const options = { year: 'numeric', month: 'long', day: 'numeric' };
  if(!date) return '';
  if(typeof date === 'string') date = new Date(date);
  return date.toLocaleDateString('en-US', options);
};

export const formatShortDate = (date) => {
  // Format javascript date as string like "June 23"
  const options = { month: 'short', day: 'numeric' };
  if(!date) return '';
  if(typeof date === 'string') date = new Date(date);
  return date.toLocaleDateString('en-US', options);
};

export const parseDateWithNoTime = (date) => {
  // Parse javascript date from string like "2023-07-31" with no time
  if(!date) return '';
  const dateParts = date.split('-');

  if(dateParts.length !== 3) {
    return '';
  }
  const dayPart = dateParts[2].length > 2 ? dateParts[2].substring(0, 2) : dateParts[2]; // Some dates come through with timezone, some don't
  return new Date(dateParts[0], dateParts[1] - 1, dayPart);
  // return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}