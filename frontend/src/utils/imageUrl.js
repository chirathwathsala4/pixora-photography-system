const BACKEND_URL = 'http://localhost:8080';

export const getImageUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http://localhost:8089')) {
    return url.replace('http://localhost:8089', BACKEND_URL);
  }
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  const cleanPath = url.startsWith('/') ? url : `/${url}`;
  return `${BACKEND_URL}${cleanPath}`;
};

export const downloadImage = async (url, customFilename) => {
  try {
    const fullUrl = getImageUrl(url);
    const response = await fetch(fullUrl);
    if (!response.ok) throw new Error('Network response was not ok');
    const blob = await response.blob();
    const blobUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    
    const urlFilename = fullUrl.substring(fullUrl.lastIndexOf('/') + 1).split('?')[0];
    link.download = customFilename || urlFilename || 'pixora-photo.jpg';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(blobUrl);
  } catch (error) {
    console.error('Direct download failed, opening in new tab', error);
    window.open(getImageUrl(url), '_blank');
  }
};